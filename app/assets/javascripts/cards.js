define(['vendors/knockout'], function(ko){

  var cardsIndex = {};

  var eventSource = (function() {

    var eventSource = new EventSource('/cards?sse');

    eventSource.addEventListener('message', function(e) {
      var evt = JSON.parse(e.data);
      console.log("card event", evt._typeHint, e, evt);
      switch (evt._typeHint) {
        case "board.Domain$Card$Create" :
          addCard({
            _id: evt.body.uri,
            _author: evt.author,
            _lastUpdateByEvent: evt._id,
            name: evt.body.name,
            description: evt.body.description,
            projectUri: evt.body.projectUri,
            columnUri: evt.body.columnUri
          });
          break;
        default:
          console.log("unahandled event")
      }
    }, false);

    eventSource.addEventListener('open', function(e) {
      console.log("listening to cards events", e);
    }, false);

    eventSource.addEventListener('error', function(e) {
      console.log("columns event source error", e)
    }, false);

    return eventSource;
  })();

  function cardForUri(uri) {
    if (!cardsIndex[uri]) {
      cardsIndex[uri] = ko.observable();
    }
    return cardsIndex[uri];
  }

  function addCard(card) {
    if (!cardsIndex[card._id]) cardsIndex[card._id] = ko.observable();

    cardsIndex[card._id](card);
  }

  $.ajax({
    url: '/cards',
    accepts: 'application/json'
  }).done(function(data) {
    data.forEach(function(card){
      addCard(card);
    })
  })

  return {
    forUri: cardForUri
  };

})