import React from "react";
import socketIOClient from "socket.io-client";

class Map extends React.Component {
  constructor(props) {
    super(props);
    this.hookURL = "http://localhost:3000";
    this.state = {
      agents: []
    };
  }

  componentDidMount() {
    const hookURL = this.hookURL;
    const socket = socketIOClient(hookURL);
    socket.on("update", agents => {
      this.setState({ agents: agents });
      console.log(this.state.agents);
    });
  }

  render() {
    return (
      <ul>
        {this.state.agents.map((agent, i) => {
          return (
            <li key={i}>
              Name: {agent[0]}, type: {agent[1].type}, location:{" "}
              {agent[1].location}{" "}
            </li>
          );
        })}
      </ul>
    );
  }
}

export default Map;
