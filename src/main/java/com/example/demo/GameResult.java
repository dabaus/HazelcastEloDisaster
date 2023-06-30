package com.example.demo;

import java.io.Serializable;

public record GameResult(String player, String opponent, GameResultType type)  implements Serializable {}