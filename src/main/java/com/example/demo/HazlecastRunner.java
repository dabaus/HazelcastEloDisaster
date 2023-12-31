package com.example.demo;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.JetService;
import com.hazelcast.jet.Traversers;
import com.hazelcast.jet.aggregate.AggregateOperations;
import com.hazelcast.jet.pipeline.*;
import com.hazelcast.jet.pipeline.file.FileFormat;
import com.hazelcast.jet.pipeline.file.FileSources;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionalMap;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;

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
            throw e;
        } finally {
            haze.shutdown();
        }
    }

    private Pipeline createPipeline(TransactionContext context, JetService jet) throws IOException {

        var pipe = Pipeline.create();

        // Loaded from sql table
        TransactionalMap<String, RatingEntry> playerRatings = context.getMap( "ratings" );

        // Loaded from csv
        var file = new File("./input/").getCanonicalPath();

        var games = pipe.readFrom(FileSources.files(file)
            .format(FileFormat.csv(GameEntry.class)).build())
            .flatMap( x -> {
                var isDraw = x.winnerScore()-x.loserScore() == 0;
                return Traversers.traverseStream(Stream.of(
                    new GameResult(x.winner(), x.loser(), isDraw ? GameResultType.DRAW : GameResultType.WIN),
                    new GameResult(x.loser(), x.winner(), isDraw ? GameResultType.DRAW : GameResultType.LOSS)
                ));
            });

        games.writeTo(Sinks.logger(x -> String.format("%s", x)));

        var numberOfGamesInBatch =
            games.groupingKey(x -> x.player())
                .aggregate(AggregateOperations.counting())
                .map(x -> new PlayerGames(x.getKey(), x.getValue().intValue()));

        var eloSumPerPlayer = games.mapUsingIMap("ratings", x->x.opponent(), (k, v) -> new GameDetails((GameResult)k, ((RatingEntry)v).rating()))
            .map(x ->  {
                var opponentRating = x.opponentRating();
                if(x.gameResult().type() == GameResultType.WIN) {
                    return new PlayerWithEloSum(x.gameResult().player(), opponentRating + 400);
                } else if (x.gameResult().type() == GameResultType.LOSS) {
                    return new PlayerWithEloSum(x.gameResult().player(), opponentRating - 400);
                } else {
                    return new PlayerWithEloSum(x.gameResult().player(), 0);
                }})
            .groupingKey(x -> x.player())
            .aggregate(AggregateOperations.summingLong(x -> x.eloSum()))
            .map(x -> new AbstractMap.SimpleEntry<String, EloEntry>(x.getKey(), new EloEntry(x.getKey(), x.getValue().intValue())));

        eloSumPerPlayer.writeTo(Sinks.logger(x -> String.format("%s", x)));

        // Join results
        var join = numberOfGamesInBatch.hashJoin(
            eloSumPerPlayer,
            JoinClause.<String, PlayerGames, EloEntry>joinMapEntries(PlayerGames::player),
            (x, y) -> new PlayerWithEloSumAndGames(x.player(), ((EloEntry) y).eloSum(), x.noGames())
        );
            // Merge with current data
            join.mapUsingIMap("ratings",x->x.player(), (k,v) -> {
            var e = (RatingEntry)v;
            var eloTotal = e.eloSum() + k.eloSum();
            var gamesTotal = k.games() + e.noGames();
            return new RatingEntry(e.player(), eloTotal, gamesTotal, gamesTotal == 0 ? 0 : eloTotal / gamesTotal);
        }).writeTo(Sinks.map("ratings", k -> k.player(), v ->
            // Store back data to rating table
            new RatingEntry(v.player(), v.eloSum(), v.noGames(), v.rating())
        ));

        return pipe;
    }
}
