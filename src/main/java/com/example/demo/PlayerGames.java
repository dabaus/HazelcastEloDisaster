package com.example.demo;

import java.io.Serializable;

public record PlayerGames (String playerName, int noGames)  implements Serializable {}
