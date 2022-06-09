(function() {
  if (typeof window === 'undefined') return;


  window.WonderPushInAppSDK = new Proxy({}, new function() {
    this.get = function(target, prop, receiver) {
      if (this.hasOwnProperty(prop)) return this[prop];
      var func = window._wpiam[prop];
      if (!func) {
        return undefined;
      }
      return function() {
        try {
          return Promise.resolve(func.apply(window._wpiam, Array.from(arguments)));
        } catch (e) {
          return Promise.reject(e);
        }
      };
    }.bind(this);
  });

  // Executed when the window is loaded
  var onload = function() {
    // Register event listeners on data-wonderpush-button-label elements
    document.querySelectorAll('*[data-wonderpush-button-label]').forEach(function(elt) {
      var buttonLabel = elt.dataset.wonderpushButtonLabel;
      elt.addEventListener('click', function() {
        window._wpiam.trackClick(buttonLabel);
      });
    });
    // Register event listeners on data-wonderpush-dismiss elements
    document.querySelectorAll('*[data-wonderpush-dismiss]').forEach(function(elt) {
      elt.addEventListener('click', function() {
        window._wpiam.dismiss();
      });
    });
  }
  if (document.readyState === "complete") {
    onload();
  } else {
    window.addEventListener("load", onload);
  }
})();
