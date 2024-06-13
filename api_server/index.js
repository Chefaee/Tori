const express = require("express");
const bodyParser = require("body-parser");
const app = express();
const port = 3000;

// field Index Switcher
isInField = true;
switchCounter = 0;

app.use(bodyParser.json());

app.get("/", (req, res) => {
    res.send("Hello dear Farmer. You are doing a great job. The API is working for you :)");
});

app.post("/api/checkPos", async (req, res) => {
    const lat = req.body.latitude;
    const lon = req.body.longitude;
    const timestamp = req.body.timestamp;
    const activity = req.body.activity;

    console.log(lat + ", " + lon + ", " + timestamp + ", " + activity);

    // A little logic so that the response is not always the same number
    if (switchCounter < 10){
        switchCounter ++;
    } else {
        switchCounter = 0;
        isInField = !isInField;
    }

    fieldIndex = isInField ? 73 : -1;

    try {
        //const result = await db(receivedString);
        res.json({
            fieldIndex: fieldIndex
        });
    } catch (error) {
        // lol
    }

});

app.head("/api/checkPos", (req, res) => {
    res.sendStatus(200); // For Testing if Api ifs available
});

app.listen(port, () => {
    console.log(`Server listening on port ${port}`);
});

