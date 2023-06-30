package com.example.demo;

import java.io.Serializable;

public record PlayerGames (String player, int noGames)  implements Serializable {}
