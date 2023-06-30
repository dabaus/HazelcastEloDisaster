package com.example.demo;

import java.io.Serializable;

public record RatingEntry(String player, Integer eloSum, Integer noGames, Integer rating) implements Serializable {
}
