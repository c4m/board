define(["vendors/knockout", "cards"], function(ko, cards) {

  var columnsIndex = {};

  var eventSource = (function() {

    var eventSource = new EventSource('/columns?sse');

    eventSource.addEventListener('message', function(e) {
      var evt = JSON.parse(e.data);
      console.log("column event", evt._typeHint, e, evt);
      switch (evt._typeHint) {
        case "board.Domain$Column$Create" :
          addColumn({
            _id: evt.body.uri,
            _author: evt.author,
            _lastUpdateByEvent: evt._id,
            name: evt.body.name,
            projectUri: evt.body.projectUri
          });
          break;
        case "board.Domain$Column$CardAdded" :
          columnsIndex[evt.body.columnUri]().cardUris.push(evt.body.cardUri);
          break;
        default:
          console.log("unahandled event")
      }
    }, false);

    eventSource.addEventListener('open', function(e) {
      console.log("listening to columns events", e);
    }, false);

    eventSource.addEventListener('error', function(e) {
      console.log("columns event source error", e)
    }, false);

    return eventSource;
  })();

  function columnForUri(uri) {
    if (!columnsIndex[uri]) {
      columnsIndex[uri] = ko.observable();
    }
    return columnsIndex[uri];
  }

  function cardForm(column) {
    var form = {
      name: ko.observable(""),
      description: ko.observable("")
    }

    form.submit = function() {
      $.ajax({
        type: 'POST',
        url: '/cards',
        accepts: 'application/json',
        contentType: 'application/json',
        data: JSON.stringify({
          columnUri: column._id,
          projectUri: column.projectUri,
          name: form.name(),
          description: form.description()
        })
      }).done(function(data) {
        form.name("");
        form.description("");
      })
    }

    return form;
  }

  function formatColumn(column) {
    column.cardForm = cardForm(column);
    column.cardUris = ko.observableArray(column.cards || []);
    column.cards = ko.computed(function(){
      return column.cardUris().map(function(uri) {
        return cards.forUri(uri);
      });
    })
  }

  function addColumn(column) {
    formatColumn(column);
    if (!columnsIndex[column._id]) columnsIndex[column._id] = ko.observable();

    columnsIndex[column._id](column);
  }

  $.ajax({
    url: '/columns',
    accept: 'application/json'
  }).done(function(data) {
    data.forEach(function(column){
      addColumn(column);
    })
  })

  return {
    forUri: columnForUri
  };

})