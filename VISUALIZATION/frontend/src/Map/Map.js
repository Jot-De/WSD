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
      agents: []
    };
  }

  componentDidMount() {
    const hookURL = this.hookURL;
    const socket = socketIOClient(hookURL);
    socket.on("update", agents => {
      this.updateBoard(true);
      console.log(agents);
      this.setState({ agents: agents }, this.updateBoard);
      if (Object.keys(this.state.agents).length === 0) {
        this.initializeGrid();
      }
    });

    this.initializeGrid();
  }

  updateBoard = (clearData = false) => {
    const grid = this.state.grid;
    const agents = this.state.agents;
    agents.forEach(agent => {
      // FIXME: Please don't parse string to array here.
      const [row, col] = agent[1].location.match(/\d+/g);

      const tile = grid[row][col];
      const newTile = {
        ...tile,
        oldType: tile.type,
        type: clearData ? tile.oldType : agent[1].type
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

  initializeGrid = () => {
    const grid = getInitialGrid();
    this.setState({ grid });
  };
}

const getInitialGrid = () => {
  const grid = [];
  for (let row = 0; row < 21; row++) {
    const currentRow = [];
    for (let col = 0; col < 41; col++) {
      if (row % 5 === 0 || col % 5 === 0)
        currentRow.push(createNode(col, row, "road"));
      else currentRow.push(createNode(col, row, "grass"));
    }
    grid.push(currentRow);
  }
  console.log(grid);
  return grid;
};

const createNode = (col, row, type) => {
  const node = {
    col,
    row,
    oldType: type,
    type
  };
  return node;
};

export default Map;
