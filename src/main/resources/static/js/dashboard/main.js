import {returnItemButtonListener} from "./returnItemButtonListener.js";
import {topUpButtonListener} from "./topUpButtonListener.js";

document.addEventListener('DOMContentLoaded', () => {
    const returnItemButtons  = document.querySelectorAll('.return-item-button');
    if (returnItemButtons) returnItemButtons.forEach(returnItemButton => {
        returnItemButtonListener(returnItemButton);
    })

    const topUpButton = document.getElementById('topUpButton');

    if (topUpButton) {
        topUpButtonListener(topUpButton)
    }
})