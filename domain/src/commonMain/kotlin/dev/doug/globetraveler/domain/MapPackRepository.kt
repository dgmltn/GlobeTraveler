package dev.doug.globetraveler.domain

interface MapPackRepository {
    suspend fun load(mapId: MapId): MapPack
}
