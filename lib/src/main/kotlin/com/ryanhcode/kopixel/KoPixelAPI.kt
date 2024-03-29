package com.ryanhcode.kopixel

import com.ryanhcode.kopixel.request.*
import com.ryanhcode.kopixel.extension.getArray
import com.ryanhcode.kopixel.extension.getJsonObject
import com.ryanhcode.kopixel.extension.getString
import com.ryanhcode.kopixel.extension.toUUID
import com.ryanhcode.kopixel.guild.Guild
import com.ryanhcode.kopixel.player.OnlineStatus
import com.ryanhcode.kopixel.player.Player
import com.ryanhcode.kopixel.skyblock.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.*


/**
 * Main class for all Hypixel API functions
 *
 * @param key A Hypixel API key
 */
class KoPixelAPI private constructor(val key: String, val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {

    companion object {

        /**
         * Extension function for a CoroutineScope to create a new KoPixel API inside of it
         * @param key A Hypixel API key
         * @return A HypixelAPI instance
         */
        fun CoroutineScope.NewKoPixelAPI(key: String): KoPixelAPI = KoPixelAPI(key, this)

        /**
         * Function for creating a new KoPixel API
         * @param key A Hypixel API key
         * @return A HypixelAPI instance
         */
        fun NewKoPixelAPI(key: String): KoPixelAPI = KoPixelAPI(key)

        const val endpoint = "https://api.hypixel.net"
    }

    internal val connectionHandler = ConnectionHandler()
    internal val namesToUUIDs = hashMapOf<String, UUID>()
    internal val UUIDsToNames = hashMapOf<UUID, String>()

    /**
     * Get a players username with their UUID via Mojang API
     *
     * @param uuid The player's UUID
     * @return The player's username
     */
    fun getName(uuid: UUID) = AsyncRequest(scope) {
        getNameSync(uuid)
    }.launch()
    fun getNameSync(uuid: UUID): String {
        // Check if uuid is already cached
        if(uuid in UUIDsToNames) {
            return UUIDsToNames[uuid]!!
        }else {
            val name = connectionHandler.readJSON("https://sessionserver.mojang.com/session/minecraft/profile/$uuid")["name"].asString
            // Cache to both hashmaps
            namesToUUIDs[name] = uuid
            UUIDsToNames[uuid] = name
            return name
        }
    }

    /**
     * Get a players UUID with their username via Mojang API
     *
     * @param uuid The player's username
     * @return The player's UUID
     */
    fun getUUID(name: String) = AsyncRequest(scope) {
        getUUIDSync(name)
    }.launch()
    fun getUUIDSync(name: String): UUID {
        // Check if name is already cached
        if (name in namesToUUIDs) {
            return namesToUUIDs[name]!!
        } else {
            val uuid =
                connectionHandler.readJSON("https://api.mojang.com/users/profiles/minecraft/$name")["id"].asString.toUUID()
            // Cache to both hashmaps
            namesToUUIDs[name] = uuid
            UUIDsToNames[uuid] = name
            return uuid
        }
    }

    /**
     * Get a player with their UUID
     *
     * @param uuid The UUID of a player
     * @return A player object representing the player
     */
    fun getPlayer(uuid: UUID): AsyncRequest<Player> = AsyncRequest(scope) {
        getPlayerSync(uuid)
    }.launch()

    /**
     * Get a player with their username
     *
     * @param name The username of a player
     * @return A player object representing the player
     */
    fun getPlayer(name: String): AsyncRequest<Player> = AsyncRequest(scope) { getPlayerSync(getUUIDSync(name)) }.launch()

    /**
     * Get a player with their username synchronously
     *
     * @param name The username of a player
     * @return A player object representing the player
     */
    fun getPlayerSync(name: String) = getPlayerSync(getUUIDSync(name))

    /**
     * Get a player with their UUID synchronously
     *
     * @param uuid The UUID of a player
     * @return A player object representing the player
     */
    fun getPlayerSync(uuid: UUID) = Player(connectionHandler.hypixelJSON("$endpoint/player?key=$key&uuid=$uuid"))


    /**
     * Get a player with either a username or a UUID
     *
     * @param name A username or UUID in String form of a player
     */
    fun get(name: String) = if(name.length <= 16) getPlayer(name) else getPlayer(name.toUUID())

    /**
     * Get a player with either a username or a UUID synchronously
     *
     * @param name A username or UUID in String form of a player
     */
    fun getSync(name: String) = if(name.length <= 16) getPlayerSync(name) else getPlayerSync(name.toUUID())

    /**
     * Get a player's skyblock profiles with their UUID
     *
     * @param uuid The UUID of a player
     * @return A list of the player's skyblock profiles
     */
    fun getSkyblockProfiles(uuid: UUID) = AsyncRequest(scope) {
        getSkyblockProfilesSync(uuid)
    }.launch()

    /**
     * Get a player's skyblock profiles with their name
     *
     * @param name The username of a player
     * @return A list of the player's skyblock profiles
     */
    fun getSkyblockProfiles(name: String) = AsyncRequest(scope) {
        getSkyblockProfilesSync(getUUIDSync(name))
    }.launch()

    /**
     * Get a player's skyblock profiles with their UUID synchronously
     *
     * @param uuid The UUID of a player
     * @return A list of the player's skyblock profiles
     */
    fun getSkyblockProfilesSync(uuid: UUID): List<Profile> {
        val profiles = connectionHandler.hypixelJSON("$endpoint/skyblock/profiles?key=$key&uuid=$uuid").getArray("profiles")
        return profiles.map { Profile(it.asJsonObject) }.toList()
    }

    /**
     * Get a player's skyblock profiles with their name synchronously
     *
     * @param name The username of a player
     * @return A list of the player's skyblock profiles
     */
    fun getSkyblockProfilesSync(name: String): List<Profile> = getSkyblockProfilesSync(getUUIDSync(name))

    /**
     * Get the current online status of a player
     * @param uuid The uuid of a player
     * @return The online status of a player
     */
    fun status(uuid: UUID) = AsyncRequest(scope) {
        statusSync(uuid)
    }

    /**
     * Get the current online status of a player synchronously
     * @param uuid The uuid of a player
     * @return The online status of a player
     */
    fun statusSync(uuid: UUID): OnlineStatus {
        val json = connectionHandler.hypixelJSON("$endpoint/status?key=$key&uuid=$uuid")
        return OnlineStatus(json)
    }

    /**
     * Get the current online status of a player synchronously
     * @param name The username of a player
     * @return The online status of a player
     */
    fun status(name: String) = AsyncRequest(scope) {
        statusSync(name)
    }

    /**
     * Get the current online status of a player synchronously
     * @param name The username of a player
     * @return The online status of a player
     */
    fun statusSync(name: String) = statusSync(getUUIDSync(name))

    /**
     * Get the guild of a player
     * @param uuid UUID of a player
     * @return The guild of the player
     */
    fun guild(uuid: UUID) = AsyncRequest(scope) {
        guildSync(uuid)
    }

    /**
     * Get the guild of a player
     * @param name Username of a player
     * @return The guild of the player
     */
    fun guild(name: String) = guild(getUUIDSync(name))

    /**
     * Get the guild of a player synchronously
     * @param uuid UUID of a player
     * @return The guild of the player
     */
    fun guildSync(uuid: UUID): Guild? {
        val json = connectionHandler.hypixelJSON("$endpoint/guild?key=$key&player=$uuid")
        if(json.get("guild").isJsonNull) return null
        return Guild(json.getJsonObject("guild"))
    }

    /**
     * Get the guild of a player synchronously
     * @param name Username of a player
     * @return The guild of the player
     */
    fun guildSync(name: String) = guildSync(getUUIDSync(name))

    /**
     *  Gets the friends of a player
     *  @param uuid The UUID of a player
     *  @return A list of friends of a player
     */
    fun friends(uuid: UUID) = AsyncRequest(scope) {
        friendsSync(uuid)
    }.launch()

    /**
     *  Gets the friends of a player
     *  @param name The username of a player
     *  @return A list of friends of a player
     */
    fun friends(name: String) = friends(getUUIDSync(name))

    /**
     *  Gets the friends of a player synchronously
     *  @param uuid The UUID of a player
     *  @return A list of friends of a player
     */
    fun friendsSync(uuid: UUID): List<UUID> {
        // Read the contents of the endpoint as JSON
        val json = connectionHandler.hypixelJSON("$endpoint/friends?key=$key&uuid=$uuid")

        return json["records"].asJsonArray.map { record ->
            val uuidSender = record.asJsonObject.getString("uuidSender")
            val uuidReceiver = record.asJsonObject.getString("uuidReceiver")
            if (uuidSender == uuid.toString()) uuidReceiver.toUUID() else uuidSender.toUUID()
        }
    }

    /**
     *  Gets the friends of a player synchronously
     *  @param name The username of a player
     *  @return A list of friends of a player
     */
    fun friendsSync(name: String) = friendsSync(getUUIDSync(name))
}
