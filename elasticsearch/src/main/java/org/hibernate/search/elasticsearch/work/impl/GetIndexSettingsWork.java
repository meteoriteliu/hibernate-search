/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.settings.impl.model.IndexSettings;
import org.hibernate.search.elasticsearch.work.impl.builder.GetIndexSettingsWorkBuilder;
import org.hibernate.search.exception.AssertionFailure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GetIndexSettingsWork extends SimpleElasticsearchWork<IndexSettings> {

	private final String indexName;

	protected GetIndexSettingsWork(Builder builder) {
		super( builder );
		this.indexName = builder.indexName;
	}

	@Override
	protected IndexSettings generateResult(ElasticsearchWorkExecutionContext context,
			Response response, JsonObject parsedResponseBody) {
		JsonElement index = parsedResponseBody.get( indexName );
		if ( index == null || !index.isJsonObject() ) {
			throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested index wasn't mentioned in the result: " + parsedResponseBody );
		}

		JsonElement settings = index.getAsJsonObject().get( "settings" );
		if ( settings == null || !settings.isJsonObject() ) {
			throw new AssertionFailure( "Elasticsearch API call succeeded, but the requested settings weren't mentioned in the result: " + parsedResponseBody );
		}

		JsonElement indexSettings = settings.getAsJsonObject().get( "index" );
		if ( indexSettings != null ) {
			GsonProvider gsonProvider = context.getGsonProvider();
			return gsonProvider.getGson().fromJson( indexSettings, IndexSettings.class );
		}
		else {
			// Empty settings
			return new IndexSettings();
		}
	}

	public static class Builder
			extends SimpleElasticsearchWork.Builder<Builder>
			implements GetIndexSettingsWorkBuilder {
		private final String indexName;

		public Builder(String indexName) {
			super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
			this.indexName = indexName;
		}

		@Override
		protected ElasticsearchRequest buildRequest() {
			ElasticsearchRequest.Builder builder =
					ElasticsearchRequest.get()
					.pathComponent( indexName )
					.pathComponent( "_settings" );
			return builder.build();
		}

		@Override
		public GetIndexSettingsWork build() {
			return new GetIndexSettingsWork( this );
		}
	}
}