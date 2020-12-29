const werewolves = {
    "language": "PL"
};

$(() => {
    const roles = [
        "DOPPELGANGER",
        "WEREWOLF",
        "MINION",
        "MASON",
        "SEER",
        "ROBBER",
        "TROUBLEMAKER",
        "DRUNK",
        "INSOMNIAC",
        "VILLAGER",
        "HUNTER",
        "TANNER"
    ];

    const translations = {
        "PL": {
            "DOPPELGANGER": "Sobowtór",
            "WEREWOLF": "Wilkołak",
            "MINION": "Sługus",
            "MASON": "Mason",
            "SEER": "Jasnowidz",
            "ROBBER": "Złodziej",
            "TROUBLEMAKER": "Intrygant",
            "DRUNK": "Pijak",
            "INSOMNIAC": "Lunatyk",
            "VILLAGER": "Wieśniak",
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
        set("roles", get("roles") || ["WEREWOLF", "WEREWOLF", "SEER", "ROBBER", "TROUBLEMAKER", "VILLAGER", "VILLAGER"])
    }


    init();
});