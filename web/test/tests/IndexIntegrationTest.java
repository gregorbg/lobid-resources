/* Copyright 2017 Fabian Steeg, hbz. Licensed under the EPL 2.0 */

package tests;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.running;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import controllers.resources.Queries;
import controllers.resources.Search;
import play.Logger;

/**
 * Integration tests for functionality provided by the {@link Search} class.
 * 
 * @author Fabian Steeg (fsteeg)
 */
@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class IndexIntegrationTest extends LocalIndexSetup {

	// test data parameters, formatted as "input /*->*/ expected output"
	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		// @formatter:off
		return Arrays.asList(new Object[][] {
			{ "title:der", /*->*/ 25 },
			{ "title:Westfalen", /*->*/ 5 },
			{ "contribution.agent.label:Westfalen", /*->*/ 9 },
			{ "contribution.agent.label:Westfälen", /*->*/ 9 },
			{ "contribution.agent.id:\"http\\://d-nb.info/gnd/5265186-1\"", /*->*/ 1 },
			{ "contribution.agent.id:5265186-1", /*->*/ 0 },
			{ "contribution.agent.id:\"5265186-1\"", /*->*/ 0 },
			{ "title:Westfalen AND contribution.agent.label:Westfalen", /*->*/ 3 },
			{ "title:Westfalen OR title:Münsterland", /*->*/ 6 },
			{ "title:Westfalen OR title:Munsterland", /*->*/ 6 },
			{ "(title:Westfalen OR title:Münsterland) AND contribution.agent.id:\"http\\://d-nb.info/gnd/2019209-5\"", /*->*/ 1 },
			{ "subject.componentList.label:Münsterland", /*->*/ 1 },
			{ "subject.componentList.label:Muensterland", /*->*/ 1 },
			{ "subject.componentList.label:Munsterland", /*->*/ 1 },
			{ "subject.componentList.label:Münsterländer", /*->*/ 1 },
			{ "subject.componentList.label.unstemmed:Münsterländer", /*->*/ 0 },
			{ "subjectAltLabel:Südwestfalen", /*->*/ 1 },
			{ "subjectAltLabel:Suedwestfalen", /*->*/ 1 },
			{ "subjectAltLabel:Sudwestfalen", /*->*/ 1 },
			{ "subjectAltLabel:Südwestfale", /*->*/ 1 },
			{ "subjectAltLabel.unstemmed:Südwestfale", /*->*/ 0 },
			{ "subject.componentList.id:\"http\\://d-nb.info/gnd/4042570-8\"", /*->*/ 6 },
			{ "(title:Westfalen OR title:Münsterland) AND NOT contribution.agent.id:\"http\\://d-nb.info/gnd/2019209-5\"", /*->*/ 5 },
			{ "subject.componentList.label:Westfalen", /*->*/ 12 },
			{ "subject.componentList.label:Westfälen", /*->*/ 12 },
			{ "spatial.label:Westfalen", /*->*/ 13 },
			{ "spatial.label:Westfälen", /*->*/ 13 },
			{ "subject.componentList.id:\"http\\://d-nb.info/gnd/4042570-8\"", /*->*/ 6 },
			{ "subject.componentList.id:1113670827", /*->*/ 0 },
			{ "subject.componentList.type:PlaceOrGeographicName", /*->*/ 35 },
			{ "publication.location:Berlin", /*->*/ 19 },
			{ "subject.notation:914.3", /*->*/ 5 },
			{ "subject.notation:914", /*->*/ 0 },
			{ "subject.notation:914*", /*->*/ 5 },
			{ "publication.location:Köln", /*->*/ 12 },
			{ "publication.location:Koln", /*->*/ 12 },
			{ "publication.startDate:1993", /*->*/ 3 },
			{ "publication.location:Berlin AND publication.startDate:1993", /*->*/ 1 },
			{ "publication.location:Berlin AND publication.startDate:[1992 TO 2017]", /*->*/ 14 },
			{ "inCollection.id:\"http\\://lobid.org/resources/HT014176012#\\!\"", /*->*/ 44 },
			{ "inCollection.id:NWBib", /*->*/ 0 },
			{ "publication.publishedBy:Springer", /*->*/ 4 },
			{ "publication.publishedBy:Spring", /*->*/ 4 },
			{ "publication.publishedBy:DAG", /*->*/ 1 },
			{ "publication.publishedBy:DÄG", /*->*/ 1 },
			{ "hasItem.id:\"http\\://lobid.org/items/TT003059252\\:DE-5-58\\:9%2F041#\\!\"", /*->*/ 1 },
			{ "hasItem.id:TT003059252\\:DE-5-58\\:9%2F041", /*->*/ 0 },
			{ "coverage:99", /*->*/ 23},
			{ "isbn:3454128013", /*->*/ 1},
			{ "isbn:345-4128-013", /*->*/ 1}
		});
	} // @formatter:on

	private int expectedResultCount;
	private Search index;

	public IndexIntegrationTest(String queryString, int resultCount) {
		this.expectedResultCount = resultCount;
		this.index = new Search.Builder()
				.query(new Queries.Builder().q(queryString).build()).build();
	}

	@Test
	public void testResultCount() {
		running(fakeApplication(), () -> {
			long totalHits = index.totalHits();
			Logger.debug("{}", index.getResult());
			assertThat(totalHits).isEqualTo(expectedResultCount);
		});
	}

}
