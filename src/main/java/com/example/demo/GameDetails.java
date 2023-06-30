package com.example.demo;

import java.io.Serializable;

public record GameDetails(GameResult gameResult, Integer opponentRating) implements Serializable {}
