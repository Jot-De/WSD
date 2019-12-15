const express = require("express");

const app = express();
app.use(express.json());

const http = require("http").createServer(app);
const io = require("socket.io")(http);

const port = 3000;

// Current state of the system.
const currentState = {};

/**
 * Parse data for frontend.
 */
const parseData = () => Object.entries(currentState).sort();

/**
 * Send data to the frontend using socket emit.
 */
const sendData = () => {
  io.emit("update", parseData());
};

/**
 * Send current data on socket connection.
 */
io.on("connection", () => {
  if (currentState) io.emit("update", parseData());
});
/**
 * Endpoint that let agents update its state.
 */
app.post("/agent", (req, res) => {
  const { name, type, location } = req.body;
  currentState[name] = { type, location };

  sendData();
  res.sendStatus(200);
});

app.delete("/agent/:name", (req, res) => {
  delete currentState[req.params.name];

  sendData();
  res.sendStatus(200);
});

/**
 * Let's go.
 */
http.listen(port, () => {
  console.log(`listening on port ${port}`);
});
