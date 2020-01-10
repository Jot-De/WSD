import React from "react";
import socketIOClient from "socket.io-client";

import Tile from "./Tile/Tile";
import "./Map.css";

class Map extends React.Component {
  constructor(props) {
    super(props);
    this.hookURL = "http://localhost:3000";
    this.state = {
      grid: [],
      agents: [
        ["car1", { type: "car", location: "[0, 1]" }],
        ["car2", { type: "car", location: "[0, 5]" }],
        ["parking1", { type: "parking", location: "[2, 10]" }]
      ]
    };
  }

  componentDidMount() {
    const hookURL = this.hookURL;
    const socket = socketIOClient(hookURL);
    socket.on("update", agents => {
      this.updateBoard(true);
      this.setState({ agents: agents }, this.updateBoard);
    });

    const grid = getInitialGrid();
    this.setState({ grid });
  }

  updateBoard = (clearData = false) => {
    const grid = this.state.grid;
    const agents = this.state.agents;
    agents.forEach(agent => {
      // FIXME: Please don't parse string to array here.
      const [row, col] = agent[1].location.match(/\d+/g);
      const type = clearData ? "none" : agent[1].type;

      const tile = grid[row][col];
      const newTile = {
        ...tile,
        type: type
      };
      grid[row][col] = newTile;
    });
    this.setState({ grid });
  };

  render() {
    return (
      <div className="grid">
        {this.state.grid.map((row, rowIdx) => {
          return (
            <div className="row" key={rowIdx}>
              {row.map((tile, tileIdx) => {
                const { type } = tile;
                return (
                  <Tile
                    key={tileIdx}
                    name={`${rowIdx}.${tileIdx}`}
                    type={type}
                  />
                );
              })}
            </div>
          );
        })}
      </div>
    );
  }
}

const getInitialGrid = () => {
  const grid = [];
  for (let row = 0; row < 20; row++) {
    const currentRow = [];
    for (let col = 0; col < 20; col++) {
      currentRow.push(createNode(col, row));
    }
    grid.push(currentRow);
  }
  return grid;
};

const createNode = (col, row) => {
  return {
    col,
    row,
    type: "none"
  };
};

export default Map;
