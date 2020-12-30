const werewolves = {
    "language": "PL"
};

$(() => {
    const roles = [
        "DOPPELGANGER",
        "WEREWOLF_1",
        "WEREWOLF_2",
        "WEREWOLF_3",
        "MINION",
        "MASON_1",
        "MASON_2",
        "SEER",
        "ROBBER",
        "TROUBLEMAKER",
        "DRUNK",
        "INSOMNIAC",
        "VILLAGER_1",
        "VILLAGER_2",
        "VILLAGER_3",
        "HUNTER",
        "TANNER"
    ];

    const translations = {
        "PL": {
            "DOPPELGANGER": "Sobowtór",
            "WEREWOLF_1": "Wilkołak 1",
            "WEREWOLF_2": "Wilkołak 2",
            "MINION": "Sługus",
            "MASON_1": "Mason 1",
            "MASON_2": "Mason 2",
            "SEER": "Jasnowidz",
            "ROBBER": "Złodziej",
            "TROUBLEMAKER": "Intrygant",
            "DRUNK": "Pijak",
            "INSOMNIAC": "Lunatyk",
            "VILLAGER_1": "Wieśniak 1",
            "VILLAGER_2": "Wieśniak 2",
            "VILLAGER_3": "Wieśniak 3",
            "HUNTER": "Myśliwy",
            "TANNER": "Grabarz",
            "Player": "Gracz"
        }
    };

    const order = [
        ""
    ];

    function get(key) {
        return JSON.parse(localStorage.getItem("werewolves-" + key));
    }

    function set(key, value) {
        return localStorage.setItem("werewolves-" + key, JSON.stringify(value));
    }

    function translate(key) {
        return translations[werewolves.language][key] || key
    }

    function init() {
        set("name", get("name") || translate("Player"));
        set("id", get("id") || Math.random().toString().substr(-8));
        set("game", get("game") || "game");
        set("roles", get("roles") || ["WEREWOLF_1", "WEREWOLF_2", "SEER", "ROBBER", "TROUBLEMAKER", "VILLAGER_1", "VILLAGER_2"])
    }


    init();
});