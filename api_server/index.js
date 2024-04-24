const express = require("express");
const bodyParser = require("body-parser");
const app = express();
const port = 3000;

app.use(bodyParser.json());

app.get("/", (req, res) => {
    res.send("Hello dear Farmer. You are doing a great job. The API is working for you :)");
});

app.listen(port, () => {
    console.log(`Server listening on port ${port}`);
});