package com.example.demo;

import java.io.Serializable;

public record GameEntry(String game, String winner, String loser, int winnerScore, int loserScore) implements Serializable {}

