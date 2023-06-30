package com.example.demo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.pipeline.file.FileFormat;
import com.hazelcast.jet.pipeline.file.FileSources;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class HazlecastRunner implements CommandLineRunner {

    private HazelcastInstance haze;

    public HazlecastRunner(HazelcastInstance hazelcast) {
        this.haze = hazelcast;
    }

    @Override
    public void run(String... args) throws Exception {
        TransactionContext context = null;
        try {
            var jet = haze.getJet();
            TransactionOptions options = new TransactionOptions()
                .setTransactionType(TransactionOptions.TransactionType.TWO_PHASE);
            context = haze.newTransactionContext(options);
            context.beginTransaction();

            jet.newJob(createPipeline(context, jet))
                .join();

            context.commitTransaction();
        } catch (Exception e) {
            if(context != null) {
                context.rollbackTransaction();
            }
        } finally {
            haze.shutdown();
        }
    }

    private Pipeline createPipeline(TransactionContext context, JetService jet) throws IOException {

        var pipe = Pipeline.create();

        // Loaded from sql table
        var playerRatings = context.getMap( "ratings" );

        // Loaded from csv
        var file = new File("./input/").getCanonicalPath();


        pipe.readFrom(FileSources.files(file)
            .format(FileFormat.csv(GameEntry.class)).build())
            .mapUsingIMap("ratings", GameEntry::winner, (game, rating) -> new GameDetails(game, (int)rating, 0))
            .mapUsingIMap("ratings", x -> x.gameEntry().loser(),
                (details, rating) -> new GameDetails(details.gameEntry(), details.winnerRating(), (int)rating))
            .writeTo(Sinks.logger(x -> String.format("%s", x)));;

        return pipe;
    }
}
