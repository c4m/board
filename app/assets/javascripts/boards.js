define(['vendors/knockout', 'columns'], function(ko, columns) {

  var boardsIndex = {};

  var boards = ko.observableArray([]);

  var selected = ko.observable();

  var current = ko.observable();

  selected.subscribe(function(selected) {
    if (selected && boardsIndex[selected]) {
      current(boardsIndex[selected]);
    } else {
      current(null);
    }
  });

  var eventSource = (function() {

    var eventSource = new EventSource('/boards?sse');

    eventSource.addEventListener('message', function(e) {
      var evt = JSON.parse(e.data);
      console.log("board event", evt._typeHint, e, evt);
      switch (evt._typeHint) {
        case "board.Domain$Board$Create" :
          addBoard({
            _id: evt.body.uri,
            _author: evt.author,
            _lastUpdateByEvent: evt._id,
            name: evt.body.name
          });
          break;
        case "board.Domain$Board$ColumnAdded" :
          boardsIndex[evt.body.projectUri].columnUris.push(evt.body.columnUri)
          break;
        default:
          console.log("unahandled event")
      }
    }, false);

    eventSource.addEventListener('open', function(e) {
      console.log("listening to boards events", e);
    }, false);

    eventSource.addEventListener('error', function(e) {
      console.log("board event source error", e)
    }, false);

    return eventSource;
  })();

  var newBoard = {
    name: ko.observable("")
  };

  newBoard.submit = function() {
    $.ajax({
      type: 'POST',
      url: '/boards',
      accept: 'application/json',
      contentType: 'application/json',
      data: JSON.stringify({
        name: newBoard.name()
      })
    }).done(function(data) {
      newBoard.name("");
    })
  }

  var newColumn = {
    name: ko.observable("")
  }

  newColumn.submit = function() {
    $.ajax({
      type: 'POST',
      url: '/columns',
      accepts: 'application/json',
      contentType: 'application/json',
      data: JSON.stringify({
        projectUri: selected(),
        name: newColumn.name()
      })
    }).done(function(data) {
      newColumn.name("");
    })
  }

  function formatBoard(board) {
    board.href = "#"+board._id;
    board.columnUris = ko.observableArray(board.columns || []);
    board.columns = ko.computed(function() {
      return board.columnUris().map(function(uri) {
        return columns.forUri(uri);
      })
    });
  }

  function addBoard(board) {
    formatBoard(board);
    boardsIndex[board._id] = board;
    boards.push(board);
  }

  $.ajax({
    url: '/boards',
    accept: 'application/json'
  }).done(function(data) {
    data.forEach(function(board){
      addBoard(board);
    })
  })

  return {
    all: boards,
    selected: selected,
    current: current,
    index: boardsIndex,
    newBoard: newBoard,
    newColumn: newColumn
  };

}); 