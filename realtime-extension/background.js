chrome.action.onClicked.addListener(async (tab) => {
  // Deliberately no automatic capture. The extension should open RakshaCall's
  // consent UI and require a user gesture before tab/media capture begins.
  await chrome.tabs.create({url: "http://localhost:5173/?capture=consent"});
});
