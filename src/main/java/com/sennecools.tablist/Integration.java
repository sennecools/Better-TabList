package com.sennecools.tablist;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import dev.ftb.mods.ftbranks.api.RankManager;

public class Integration {
    public static FTBRanksAPI getFTBRanksAPI() {
        return FTBRanksAPI.getInstance();
    }
    public static RankManager getRankManager() {
        return FTBRanksAPI.manager();
    }
}
