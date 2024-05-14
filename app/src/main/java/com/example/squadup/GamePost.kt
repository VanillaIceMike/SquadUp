package com.example.squadup

data class GamePost(
    val sportType: String = "",
    val numPlayers: Int = 0,
    val timeframe: String = "",
    val location: Map<String, Double> = mapOf(),
    val userInfo: Map<String, String> = mapOf()
)
