package com.example.demo;

import java.io.Serializable;

public record PlayerWithEloSumAndGames(String player, Integer eloSum, Integer games)  implements Serializable {
}
