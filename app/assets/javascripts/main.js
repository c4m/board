require.config({
    paths: {
        
    }
});


require(['vendors/director', 'vendors/knockout', 'boards'],function(director, ko, boards) {

  var viewmodel = {
    page: ko.observable("#/boards"),
    boards: boards
  };

  var boards = function() { 
        viewmodel.page("#/boards");
        viewmodel.boards.selected(null);
      },
      board = function(boardId) {
        viewmodel.page("#/boards/"+boardId);
        viewmodel.boards.selected("/boards/"+boardId);
      };

  var routes = {
    '/boards': boards,
    '/boards/:boardId': board
  };

  var router = director.Router(routes);
  router.init();

  if (window.location.hash === "") window.location.hash = "/boards";
  else {
    var oldHash = window.location.hash;
    window.location.hash = "/boards";
    window.location.hash = oldHash;
  }

  ko.applyBindings(viewmodel);

});