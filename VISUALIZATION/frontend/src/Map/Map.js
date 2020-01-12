import React from "react";
import socketIOClient from "socket.io-client";

import Tile from "./Tile/Tile";
import "./Map.css";

class Map extends React.Component {
  constructor(props) {
    super(props);
    this.hookURL = "http://localhost:3000";
    this.cache = {};
    this.state = {
      grid: [],
      agents: []
    };
  }

  componentDidMount() {
    this.initializeGrid();
    const hookURL = this.hookURL;
    const socket = socketIOClient(hookURL);
    socket.on("update", agents => {
      this.setState({ agents: agents }, this.updateBoard);
    });
  }

  updateBoard = () => {
    const grid = this.getInitialGrid();
    const agents = this.state.agents;
    agents.forEach(agent => {
      // FIXME: Please don't parse string to array here.
      const [row, col] = agent[1].location.match(/\d+/g);

      const tile = grid[row][col];
      const newTile = {
        ...tile,
        isParking: agent[1].type === "parking",
        isCar: agent[1].type === "car",
        freeSlots: agent[1].freeSlots
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
                const { type, isParking, isCar, freeSlots } = tile;
                return (
                  <Tile
                    key={tileIdx}
                    name={`${rowIdx}.${tileIdx}`}
                    type={type}
                    isParking={isParking}
                    isCar={isCar}
                    freeSlots={freeSlots}
                  />
                );
              })}
            </div>
          );
        })}
      </div>
    );
  }

  initializeGrid = () => {
    const grid = this.getInitialGrid();
    this.setState({ grid });
  };

  getInitialGrid = () => {
    if (!this.cache.initialGrid) {
      const grid = [];
      for (let row = 0; row < 21; row++) {
        const currentRow = [];
        for (let col = 0; col < 41; col++) {
          if (row % 5 === 0 || col % 5 === 0)
            currentRow.push(createTile(col, row, "road"));
          else
            currentRow.push(createTile(col, row, `grass ${getRandomType()}`));
        }
        grid.push(currentRow);
      }
      this.cache.initialGrid = grid;
      return grid;
    } else {
      const grid = [];
      const { initialGrid } = this.cache;
      for (const row of initialGrid) {
        grid.push([...row]);
      }
      return grid;
    }
  };
}

const createTile = (col, row, type) => {
  const tile = {
    col,
    row,
    type,
    isParking: false,
    isCar: false
  };
  return tile;
};

export default Map;

const getRandomType = () => {
  const types = ["tree1", "tree2", "house"];
  const rand = Math.floor(Math.random() * types.length);
  return types[rand];
};
