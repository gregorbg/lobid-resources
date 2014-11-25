/* Copyright 2014 Fabian Steeg, hbz. Licensed under the GPLv2 */

package controllers.nwbib;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import play.Logger;
import play.cache.Cache;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.html.HtmlEscapers;

/**
 * Access Lobid title data.
 *
 * @author fsteeg
 *
 */
public class Lobid {

	static Long getTotalResults(JsonNode json) {
		return json.findValue("http://sindice.com/vocab/search#totalResults")
				.asLong();
	}

	static WSRequestHolder request(final String q, final String author,
			final String name, final String subject, final String id,
			final String publisher, final String issued, final String medium,
			final String nwbibspatial, final String nwbibsubject, final int from,
			final int size, String owner, String t, String sort, boolean allData,
			String set, String location) {
		WSRequestHolder requestHolder =
				WS.url(Application.CONFIG.getString("nwbib.api"))
						.setHeader("Accept", "application/json")
						.setQueryParameter("format", "full")
						.setQueryParameter("from", from + "")
						.setQueryParameter("size", size + "")
						.setQueryParameter("sort", sort)
						.setQueryParameter("location", locationPolygon(location));
		if (!allData && set.isEmpty())
			requestHolder =
					requestHolder.setQueryParameter("set",
							Application.CONFIG.getString("nwbib.set"));
		if (!set.isEmpty())
			requestHolder = requestHolder.setQueryParameter("set", set);
		if (!q.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("q", preprocess(q));
		if (!author.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("author", author);
		if (!name.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("name", name);
		if (!subject.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("subject", subject);
		if (!id.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("id", id);
		if (!publisher.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("publisher", publisher);
		if (!issued.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("issued", issued);
		if (!medium.trim().isEmpty())
			requestHolder = requestHolder.setQueryParameter("medium", medium);
		if (!nwbibspatial.trim().isEmpty())
			requestHolder =
					requestHolder.setQueryParameter("nwbibspatial", nwbibspatial);
		if (!nwbibsubject.trim().isEmpty())
			requestHolder =
					requestHolder.setQueryParameter("nwbibsubject", nwbibsubject);
		if (!owner.isEmpty())
			requestHolder = requestHolder.setQueryParameter("owner", owner);
		if (!t.isEmpty())
			requestHolder = requestHolder.setQueryParameter("t", t);
		Logger.info("Request URL {}, query params {} ", requestHolder.getUrl(),
				requestHolder.getQueryParameters());
		return requestHolder;
	}

	static WSRequestHolder topicRequest(final String q) {
		WSRequestHolder requestHolder =// @formatter:off
				WS.url(Application.CONFIG.getString("nwbib.api"))
						.setHeader("Accept", "application/json")
						.setQueryParameter("format", "short.subjectChain")
						.setQueryParameter("from", "" + 0)
						.setQueryParameter("size", "" + 100)
						.setQueryParameter("subject", q)
						.setQueryParameter("set",
							Application.CONFIG.getString("nwbib.set"));
		//@formatter:off
		Logger.info("Request URL {}, query params {} ", requestHolder.getUrl(),
				requestHolder.getQueryParameters());
		return requestHolder;
	}

	/** @return The full number of hits, ie. the size of our data set. */
	public static Promise<Long> getTotalHits() {
		final Long cachedResult = (Long) Cache.get(String.format("totalHits"));
		if (cachedResult != null) {
			return Promise.promise(() -> {
				return cachedResult;
			});
		}
		WSRequestHolder requestHolder =
				request("", "", "", "", "", "", "", "", "", "", 0, 0, "", "", "", false, "", "");
		return requestHolder.get().map((WSResponse response) -> {
			Long total = getTotalResults(response.asJson());
			Cache.set("totalHits", total, Application.ONE_HOUR);
			return total;
		});
	}

	/**
	 * @param uri A Lobid-Organisation URI
	 * @return A human readable label for the given URI
	 */
	public static String organisationLabel(String uri) {
		String cacheKey = "org.label." + uri;
		String format = "short.altLabel";
		return lobidLabel(uri, cacheKey, format, true);
	}

	/**
	 * @param uri A Lobid-Resources URI
	 * @return A human readable label for the given URI
	 */
	public static String resourceLabel(String uri) {
		String cacheKey = "res.label." + uri;
		String format = "short.title";
		return lobidLabel(uri, cacheKey, format, false);
	}

	private static String lobidLabel(String uri, String cacheKey, String format, boolean shorten) {
		final String cachedResult = (String) Cache.get(cacheKey);
		if (cachedResult != null) {
			return cachedResult;
		}
		WSRequestHolder requestHolder =
				WS.url(uri).setHeader("Accept", "application/json")
						.setQueryParameter("format", format);
		return requestHolder.get().map((WSResponse response) -> {
			Iterator<JsonNode> elements = response.asJson().elements();
			String label = "";
			if (elements.hasNext()) {
				String full = elements.next().asText();
				label = shorten ? shorten(full) : full;
			} else {
				label = uri.substring(uri.lastIndexOf('/') + 1);
			}
			label = HtmlEscapers.htmlEscaper().escape(label);
			Cache.set(cacheKey, label);
			return label;
		}).get(10000);
	}

	private static String gndLabel(String uri) {
		String cacheKey = "gnd.label." + uri;
		final String cachedResult = (String) Cache.get(cacheKey);
		if (cachedResult != null) {
			return cachedResult;
		}
		WSRequestHolder requestHolder =
				WS.url("http://api.lobid.org/subject").setHeader("Accept", "application/json")
						.setQueryParameter("id", uri)
						.setQueryParameter("format", "full");
		return requestHolder.get().map((WSResponse response) -> {
			JsonNode value = response.asJson().findValue("preferredName");
			String label = "";
			if(value != null) {
				label = shorten(value.asText());
			} else {
				label = uri.substring(uri.lastIndexOf('/') + 1);
			}
			label = HtmlEscapers.htmlEscaper().escape(label);
			Cache.set(cacheKey, label);
			return label;
		}).get(10000);
	}

	private static String nwBibLabel(String uri) {
		String cacheKey = "nwbib.label." + uri;
		final String cachedResult = (String) Cache.get(cacheKey);
		if (cachedResult != null) {
			return cachedResult;
		}
		String type =
				uri.contains("spatial") ? Classification.Type.SPATIAL.elasticsearchType
						: Classification.Type.NWBIB.elasticsearchType;
		String label = Application.CLASSIFICATION.label(uri, type);
		label = shorten(label);
		label = HtmlEscapers.htmlEscaper().escape(label);
		Cache.set(cacheKey, label);
		return label;
	}

	private static String shorten(String label) {
		int limit = 45;
		if (label.length() > limit)
			return label.substring(0, limit) + "...";
		return label;
	}

	/**
	 * @param q Query to search in all fields
	 * @param author Query for the resource author
	 * @param name Query for the resource name (title)
	 * @param subject Query for the resource subject
	 * @param id Query for the resource id
	 * @param publisher Query for the resource author
	 * @param issued Query for the resource issued year
	 * @param medium Query for the resource medium
	 * @param nwbibspatial Query for the resource nwbibspatial classification
	 * @param nwbibsubject Query for the resource nwbibsubject classification
	 * @param owner Owner filter for resource queries
	 * @param t Type filter for resource queries
	 * @param field The facet field (the field to facet over)
	 * @param set The set, overrides the default NWBib set if not empty
	 * @param location A polygon describing the subject area of the resources
	 * @return A JSON representation of the requested facets
	 */
	public static Promise<JsonNode> getFacets(String q, String author,
			String name, String subject, String id, String publisher, String issued,
			String medium, String nwbibspatial, String nwbibsubject, String owner,
			String field, String t, String set, String location) {
		WSRequestHolder request =
				WS.url(Application.CONFIG.getString("nwbib.api") + "/facets")
						.setHeader("Accept", "application/json")
						.setQueryParameter("q", preprocess(q))
						.setQueryParameter("author", author)
						.setQueryParameter("name", name)
						.setQueryParameter("publisher", publisher)
						.setQueryParameter("issued", issued).setQueryParameter("id", id)
						.setQueryParameter("field", field).setQueryParameter("from", "0")
						.setQueryParameter("size", Application.MAX_FACETS+"")
						.setQueryParameter("location", locationPolygon(location));
		if(!set.isEmpty())
			request = request.setQueryParameter("set", set);
		else
			request = request.setQueryParameter("set", Application.CONFIG.getString("nwbib.set"));
		if (!field.equals(Application.MEDIUM_FIELD))
			request = request.setQueryParameter("medium", medium);
		if (!field.equals(Application.TYPE_FIELD))
			request = request.setQueryParameter("t", t);
		if (!field.equals(Application.ITEM_FIELD))
			request = request.setQueryParameter("owner", owner);
		if (!field.equals(Application.NWBIB_SPATIAL_FIELD))
			request = request.setQueryParameter("nwbibspatial", nwbibspatial);
		if (!field.equals(Application.NWBIB_SUBJECT_FIELD))
			request = request.setQueryParameter("nwbibsubject", nwbibsubject);
		if(!field.equals(Application.SUBJECT_FIELD))
			request = request.setQueryParameter("subject", subject);
		Logger.info("Facets request URL {}, query params {} ", request.getUrl(),
				request.getQueryParameters());
		return request.get().map((WSResponse response) -> {
			return response.asJson();
		});
	}

	private static String preprocess(final String q) {
		return // if query string syntax is used, leave it alone:
		q.trim().isEmpty() || q.matches(".*?([+~]|AND|OR|\\s-|\\*).*?") ? q :
		// else prepend '+' to all terms for AND search:
				Arrays.asList(q.split("[\\s-]")).stream().map(x -> "+" + x)
						.collect(Collectors.joining(" "));
	}

	private static final Map<String, String> keys = ImmutableMap.of(
			Application.TYPE_FIELD, "type.labels",//
			Application.MEDIUM_FIELD, "medium.labels");

	/**
	 * @param types Some type URIs
	 * @return An icon CSS class for the URIs
	 */
	public static String typeIcon(List<String> types) {
		return facetIcon(types, Application.TYPE_FIELD);
	}

	/**
	 * @param types Some type URIs
	 * @return A human readable label for the URIs
	 */
	public static String typeLabel(List<String> types) {
		return facetLabel(types, Application.TYPE_FIELD);
	}

	/**
	 * @param uris Some URIs
	 * @param field The ES field to facet over
	 * @return A human readable label for the URIs
	 */
	public static String facetLabel(List<String> uris, String field) {
		if (uris.size() == 1 && isOrg(uris.get(0)))
			return Lobid.organisationLabel(uris.get(0));
		else if (uris.size() == 1
				&& (isNwBibClass(uris.get(0)) || isNwBibSpatial(uris.get(0))))
			return Lobid.nwBibLabel(uris.get(0));
		else if (uris.size() == 1 && isGnd(uris.get(0)))
			return Lobid.gndLabel(uris.get(0));
		String configKey = keys.getOrDefault(field,"");
		String type = selectType(uris, configKey);
		if (type.isEmpty())
			return "";
		@SuppressWarnings("unchecked")
		List<String> details = configKey.isEmpty() ? uris :
				((List<String>) Application.CONFIG.getObject(configKey).unwrapped()
						.get(type));
		if (details == null || details.size() < 1)
			return type;
		String selected = details.get(0);
		return selected.isEmpty() ? uris.get(0) : selected;
	}

	/**
	 * @param uris Some URIs
	 * @param field The ES field to facet over
	 * @return An icon CSS class for the given URIs
	 */
	public static String facetIcon(List<String> uris, String field) {
		if (uris.size() == 1 && isOrg(uris.get(0)))
			return "octicon octicon-home";
		else if (uris.size() == 1 && isNwBibClass(uris.get(0)))
			return "octicon octicon-list-unordered";
		else if (uris.size() == 1 && isNwBibSpatial(uris.get(0)))
			return "octicon octicon-milestone";
		else if (uris.size() == 1 && isGnd(uris.get(0)))
			return "octicon octicon-tag";
		String configKey = keys.getOrDefault(field, "");
		String type = selectType(uris, configKey);
		if (type.isEmpty())
			return "";
		@SuppressWarnings("unchecked")
		List<String> details = configKey.isEmpty() ? uris :
				(List<String>) Application.CONFIG.getObject(configKey).unwrapped()
						.get(type);
		if (details == null || details.size() < 2)
			return type;
		String selected = details.get(1);
		return selected.isEmpty() ? uris.get(0) : selected;
	}


	private static String selectType(List<String> types, String configKey) {
		if(configKey.isEmpty())
			return types.get(0);
		Logger.trace("Types: " + types);
		@SuppressWarnings("unchecked")
		List<String> selected =
				types
						.stream()
						.map(
								t -> {
									List<String> vals =
											((List<String>) Application.CONFIG.getObject(configKey)
													.unwrapped().get(t));
									if (vals == null)
										return t;
									return vals.get(0).isEmpty() || vals.get(1).isEmpty() ? ""
											: t;
								}).filter(t -> {
							return !t.isEmpty();
						}).collect(Collectors.toList());
		Collections.sort(selected);
		Logger.trace("Selected: " + selected);
		return selected.isEmpty() ? "" : selected.get(0).contains("Miscellaneous")
				&& selected.size() > 1 ? selected.get(1) : selected.get(0);
	}

	static boolean isOrg(String term) {
		return term.startsWith("http://lobid.org/organisation");
	}

	static boolean isNwBibClass(String term) {
		return term.startsWith("http://purl.org/lobid/nwbib#");
	}

	private static boolean isNwBibSpatial(String term) {
		return term.startsWith("http://purl.org/lobid/nwbib-spatial#");
	}

	private static boolean isGnd(String term) {
		return term.startsWith("http://d-nb.info/gnd");
	}

	private static String locationPolygon(String location) {
		return location.contains("|") ? location.split("\\|")[1] : location;
	}
}
