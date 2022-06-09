(function() {
  if (typeof window === 'undefined') return;

  var convertArguments = function(args) {
    args = Array.from(args);
    return args.map(function(elt) {
      if (elt === null) return null;
      if (elt === undefined) return null;
      if (typeof elt === 'function') return null;
      if (typeof elt === 'string') return elt;
      var prefix = "__" + (typeof elt) + "__";
      if (typeof elt === 'object') return prefix + JSON.stringify(elt);
      return prefix + elt.toString();
    });

  };

  window.WonderPushInAppSDK = new Proxy({}, new function() {
    this.get = function(target, prop, receiver) {
      if (this.hasOwnProperty(prop)) return this[prop];
      var func = window._wpiam[prop];
      if (!func) {
        return undefined;
      }
      return function() {
        try {
          var result = func.apply(window._wpiam, convertArguments(arguments));
          // Array & Object results start with __array__ or __object__
          if (typeof result === 'string') {
            var match = result.match(/^__(array|object)__/);
            if (match) {
              try {
                result = JSON.parse(result.substring(match[0].length));
              } catch (e) {
                console.error(e);
              }
            }
          }
          return Promise.resolve(result);
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
