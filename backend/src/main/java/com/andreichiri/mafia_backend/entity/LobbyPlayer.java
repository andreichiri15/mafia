package com.andreichiri.mafia_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "lobby_players")
public class LobbyPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}
