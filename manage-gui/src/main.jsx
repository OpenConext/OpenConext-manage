import React from "react";
import App from "./pages/App";
import {createRoot} from 'react-dom/client';
import "typeface-roboto";
import "@fortawesome/fontawesome-free/css/all.css";

const container = document.getElementById('app');
const root = createRoot(container);
root.render(<App/>);

