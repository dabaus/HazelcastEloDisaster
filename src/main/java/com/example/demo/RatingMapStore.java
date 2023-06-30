package com.example.demo;

import com.example.demo.factory.DbConnectionFactoryImpl;
import com.hazelcast.map.MapStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
public class RatingMapStore implements MapStore<String, RatingEntry> {

    private final Connection con;
    private final PreparedStatement allKeysStatement;

    public RatingMapStore() {
        con = new DbConnectionFactoryImpl().openConnection();
        try {
            allKeysStatement = con.prepareStatement("select name from ratings");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void store(String key, RatingEntry value) {
        try {
            var sql = """
                        insert into ratings 
                        values('%s', %d, %d, %d)
                        on conflict (name) do update 
                            set elosum=EXCLUDED.elosum,
                                nogames=EXCLUDED.nogames,
                                rating=EXCLUDED.rating;
                      """;
            con.createStatement().executeUpdate(
                String.format(sql ,key, value.eloSum(), value.noGames(), value.rating()));

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void storeAll(Map<String, RatingEntry> map) {
        for (Map.Entry<String, RatingEntry> entry : map.entrySet()) {
            store(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void delete(String key) {
        try {
            con.createStatement().executeUpdate(
                String.format("delete from ratings where name = %s", key));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteAll(Collection<String> keys) {
        for (String key : keys) {
            delete(key);
        }
    }

    @Override
    public synchronized RatingEntry load(String key) {
        try {
            ResultSet resultSet = con.createStatement().executeQuery(
                String.format("select elosum, nogames, rating from ratings where name = '%s'", key));
            try {
                if (!resultSet.next()) {
                    return null;
                }

                return new RatingEntry(key,
                    resultSet.getInt(1),
                    resultSet.getInt(2),
                    resultSet.getInt(3));
            } finally {
                resultSet.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, RatingEntry> loadAll(Collection<String> keys) {
        Map<String, RatingEntry> result = new HashMap<String, RatingEntry>();
        for (String key : keys) {
            result.put(key, load(key));
        }
        return result;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        try {
            var resultSet = allKeysStatement.executeQuery();
            var list = new ArrayList<String>();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            return () -> list.iterator();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
