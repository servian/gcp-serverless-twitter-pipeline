package org.polleyg;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.extensions.sql.SqlTransform;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.joda.time.Duration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED;
import static org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition.WRITE_APPEND;

/**
 * Dataflow streaming pipeline to read tweets from PubSub topic and write the payload to BigQuery
 */
public class TweetPipeline {
    private static final String TOPIC = "projects/gcp-serverless-twitter-pipeline/topics/twitter";
    private static final String TOPIC_OUT = "projects/gcp-serverless-twitter-pipeline/topics/tweets_filtered_beam_sql";
    private static final String BIGQUERY_DESTINATION_FILTERED = "%s:twitter.tweets_filtered_by_beam_sql";
    private static final Schema SCHEMA = Schema.builder()
            .addStringField("payload")
            .addStringField("tweet")
            .build();
    private static Pattern REGEX_TWEET = Pattern.compile("\"text\":\"(.+?)\",");

    public static void main(String[] args) {
        PipelineOptionsFactory.register(DataflowPipelineOptions.class);
        DataflowPipelineOptions options = PipelineOptionsFactory
                .fromArgs(args)
                .withValidation()
                .as(DataflowPipelineOptions.class);
        Pipeline pipeline = Pipeline.create(options);

        PCollection<PubsubMessage> messages = pipeline
                .apply(PubsubIO.readMessagesWithAttributes().fromTopic(TOPIC))
                .apply(Window.into(FixedWindows.of(Duration.standardSeconds(1))));

        PCollection<Row> filteredBySql = messages
                .apply(ParDo.of(new PubSubMessageToBeamSqlRow())).setRowSchema(SCHEMA)
                .apply(SqlTransform.query("SELECT * FROM PCOLLECTION " +
                        "WHERE LOWER(tweet) LIKE '%onboard%' OR LOWER(tweet) LIKE '%data%'"));

        filteredBySql.apply(ParDo.of(new RowToString()))
                .apply(PubsubIO.writeStrings().to(TOPIC_OUT));

        filteredBySql.apply(ParDo.of(new RowToBQRow()))
                .apply(BigQueryIO.writeTableRows()
                        .to(String.format(BIGQUERY_DESTINATION_FILTERED, options.getProject()))
                        .withCreateDisposition(CREATE_IF_NEEDED)
                        .withWriteDisposition(WRITE_APPEND)
                        .withSchema(getTableSchema()));
        pipeline.run();
    }

    private static TableSchema getTableSchema() {
        List<TableFieldSchema> fields = new ArrayList<>();
        fields.add(new TableFieldSchema().setName("timestamp").setType("INTEGER"));
        fields.add(new TableFieldSchema().setName("payload").setType("STRING"));
        return new TableSchema().setFields(fields);
    }

    public static class RowToString extends DoFn<Row, String> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            System.out.println(c.element().getString("tweet"));
            c.output(c.element().getString("payload")); //spit out the entire payload
        }
    }

    public static class PubSubMessageToBeamSqlRow extends DoFn<PubsubMessage, Row> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            String payload = new String(c.element().getPayload(), StandardCharsets.UTF_8);
            Matcher m = REGEX_TWEET.matcher(payload);
            if (m.find()) { //pull out just the tweet text
                Row appRow = Row
                        .withSchema(SCHEMA)
                        .addValues(payload, m.group(1))
                        .build();
                c.output(appRow);
            }
        }
    }

    public static class RowToBQRow extends DoFn<Row, TableRow> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            c.output(new TableRow()
                    .set("timestamp", System.currentTimeMillis())
                    .set("payload", c.element().getString("payload"))
            );
        }
    }
}