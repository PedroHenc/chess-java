package chess;

import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChessMatch {

   private static int turn;
   private static Color currentPlayer;
   private static Board board;
   private static boolean check;
   private static boolean checkMate;
   private static ChessPiece enPassantVulnerable;
   private static ChessPiece promoted;


   private static List<Piece> piecesOnTheBoard = new ArrayList<>();
   private static List<Piece> capturedPieces = new ArrayList<>();

   public ChessMatch() {
	  board = new Board(8, 8);
	  turn = 1;
	  currentPlayer = Color.WHITE;
	  initialSetup();
   }

   public int getTurn() {
	  return turn;
   }

   public Color getCurrentPlayer() {
	  return currentPlayer;
   }

   public boolean getCheck() {
	  return check;
   }

   public boolean getCheckMate() {
	  return checkMate;
   }

   public ChessPiece getEnPassantVulnerable() {
	  return enPassantVulnerable;
   }

   public static ChessPiece getPromoted() {
	  return promoted;
   }

   public ChessPiece[][] getPieces() {
	  ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
	  for (int i = 0; i < board.getRows(); i++) {
		 for (int j = 0; j < board.getColumns(); j++) {
			mat[i][j] = (ChessPiece) board.piece(i, j);
		 }
	  }
	  return mat;
   }

   public boolean[][] possibleMoves(ChessPosition sourcePosition) {
	  Position position = sourcePosition.toPosition();
	  validateSourcePosition(position);
	  return board.piece(position).possibleMoves();
   }

   public static ChessPiece performChessMove(ChessPosition sourcePosition, ChessPosition targetPosition) {
	  Position source = sourcePosition.toPosition();
	  Position target = targetPosition.toPosition();
	  validateSourcePosition(source);
	  validateTargetPosition(source, target);
	  Piece capturedPiece = makeMove(source, target);

	  if (testCheck(currentPlayer)) {
		 undoMove(source, target, capturedPiece);
		 throw new ChessException("Can't put yourself in check");
	  }

	  ChessPiece MovedPiece = (ChessPiece) board.piece(target);

	  promoted = null;
	  if (MovedPiece instanceof Pawn) {
		 if ((MovedPiece.getColor() == Color.WHITE && target.getRow() == 0) || (MovedPiece.getColor() == Color.BLACK && target.getRow() == 7)) {
			promoted = (ChessPiece) board.piece(target);
			promoted = replacePromotedPiece("Q");
		 }
	  }

	  check = (testCheck(opponent(currentPlayer))) ? true : false;

	  if (testCheckMate(opponent(currentPlayer))) {
		 checkMate = true;
	  } else {
		 nextTurn();
	  }

	  if (MovedPiece instanceof Pawn && (target.getRow() == source.getRow() - 2 || target.getRow() == source.getRow() + 2)) {
		 enPassantVulnerable = MovedPiece;
	  } else {
		 enPassantVulnerable = null;
	  }

	  return (ChessPiece) capturedPiece;
   }

   public static ChessPiece replacePromotedPiece(String type) {
	  if (promoted == null) {
		 throw new IllegalStateException("Theres no piece's to be promoted");
	  }
	  if (!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) {
		return promoted;
	  }
	  Position pos = promoted.getChessPosition().toPosition();
	  Piece p = board.removePiece(pos);
	  piecesOnTheBoard.remove(p);

	  ChessPiece newPiece = newPiece(type, promoted.getColor());
	  board.placePiece(newPiece, pos);
	  piecesOnTheBoard.add(newPiece);

	  return newPiece;
   }

   private static ChessPiece newPiece(String type, Color color) {
	  if (type.equals("B")) return new Bishop(board, color);
	  if (type.equals("N")) return new Knight(board, color);
	  if (type.equals("R")) return new Rook(board, color);
	  return new Queen(board, color);
   }

   private static Piece makeMove(Position source, Position target) {
	  ChessPiece p = (ChessPiece) board.removePiece(source);
	  p.increaseMoveCount();
	  Piece capturedPiece = board.removePiece(target);
	  board.placePiece(p, target);

	  if (capturedPiece != null) {
		 piecesOnTheBoard.remove(capturedPiece);
		 capturedPieces.add(capturedPiece);
	  }

	  if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
		 Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
		 Position targetT = new Position(source.getRow(), source.getColumn() + 1);
		 ChessPiece rook = (ChessPiece) board.removePiece(sourceT);
		 board.placePiece(rook, targetT);
		 rook.increaseMoveCount();
	  }
	  if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
		 Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
		 Position targetT = new Position(source.getRow(), source.getColumn() - 1);
		 ChessPiece rook = (ChessPiece) board.removePiece(sourceT);
		 board.placePiece(rook, targetT);
		 rook.increaseMoveCount();
	  }

	  if (p instanceof Pawn) {
		 if (source.getColumn() != target.getColumn() && capturedPiece == null) {
			Position pawnPosition;
			if (p.getColor() == Color.WHITE) {
			   pawnPosition = new Position(target.getRow() + 1, target.getColumn());
			} else {
			   pawnPosition = new Position(target.getRow() - 1, target.getColumn());
			}
			capturedPiece = board.removePiece(pawnPosition);
			capturedPieces.add(capturedPiece);
			piecesOnTheBoard.remove(capturedPiece);
		 }
	  }

	  return capturedPiece;
   }


   private static void undoMove(Position source, Position target, Piece capturedPiece) {
	  ChessPiece p = (ChessPiece) board.removePiece(target);
	  p.decreaseMoveCount();
	  board.placePiece(p, source);

	  if (capturedPiece != null) {
		 board.placePiece(capturedPiece, target);
		 capturedPieces.remove(capturedPiece);
		 piecesOnTheBoard.add(capturedPiece);
	  }

	  if (p instanceof King && target.getColumn() == source.getColumn() + 2) {
		 Position sourceT = new Position(source.getRow(), source.getColumn() + 3);
		 Position targetT = new Position(source.getRow(), source.getColumn() + 1);
		 ChessPiece rook = (ChessPiece) board.removePiece(targetT);
		 board.placePiece(rook, sourceT);
		 rook.decreaseMoveCount();
	  }
	  if (p instanceof King && target.getColumn() == source.getColumn() - 2) {
		 Position sourceT = new Position(source.getRow(), source.getColumn() - 4);
		 Position targetT = new Position(source.getRow(), source.getColumn() - 1);
		 ChessPiece rook = (ChessPiece) board.removePiece(targetT);
		 board.placePiece(rook, sourceT);
		 rook.decreaseMoveCount();
	  }

	  if (p instanceof Pawn) {
		 if (source.getColumn() != target.getColumn() && capturedPiece == enPassantVulnerable) {
			ChessPiece pawn = (ChessPiece) board.removePiece(target);
			Position pawnPosition;
			if (p.getColor() == Color.WHITE) {
			   pawnPosition = new Position(3, target.getColumn());
			} else {
			   pawnPosition = new Position(4, target.getColumn());
			}
			board.placePiece(pawn, pawnPosition);

		 }
	  }
   }

   private static void validateSourcePosition(Position position) {
	  if (!board.thereIsAPiece(position)) {
		 throw new ChessException("There is no piece on source position");
	  }
	  if (currentPlayer != ((ChessPiece) board.piece(position)).getColor()) {
		 throw new ChessException("The chosen piece is not yours");
	  }
	  if (!board.piece(position).isThereAnyPossibleMove()) {
		 throw new ChessException("There any possible move for the chosen piece");
	  }
   }

   private static void validateTargetPosition(Position source, Position target) {
	  if (!board.piece(source).possibleMove(target)) {
		 throw new ChessException("The chosen piece can't move to target position");
	  }
   }

   private static void nextTurn() {
	  turn++;
	  currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
   }

   private static Color opponent(Color color) {
	  return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
   }

   private static ChessPiece king(Color color) {
	  List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color).collect(Collectors.toList());
	  for (Piece p : list) {
		 if (p instanceof Knight) {
			return (ChessPiece) p;
		 }
	  }
	  throw new IllegalStateException("The King " + color + " doesn't exit on the board");
   }

   private static boolean testCheck(Color color) {
	  Position kingPosition = king(color).getChessPosition().toPosition();
	  List<Piece> opponentPieces = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == opponent(color)).collect(Collectors.toList());
	  for (Piece p : opponentPieces) {
		 boolean[][] mat = p.possibleMoves();
		 if (mat[kingPosition.getRow()][kingPosition.getColumn()]) {
			return true;
		 }
	  }
	  return false;
   }

   private static boolean testCheckMate(Color color) {
	  if (!testCheck(color)) {
		 return false;
	  }
	  List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece) x).getColor() == color).collect(Collectors.toList());
	  for (Piece p : list) {
		 boolean[][] mat = p.possibleMoves();
		 for (int i = 0; i < board.getRows(); i++) {
			for (int j = 0; j < board.getColumns(); j++) {
			   if (mat[i][j]) {
				  Position source = ((ChessPiece) p).getChessPosition().toPosition();
				  Position target = new Position(i, j);
				  Piece capturedPiece = makeMove(source, target);
				  boolean testCheck = testCheck(color);
				  undoMove(source, target, capturedPiece);
				  if (!testCheck) {
					 return false;
				  }
			   }
			}
		 }
	  }
	  return true;
   }

   private void placeNewPiece(char column, int row, ChessPiece piece) {
	  board.placePiece(piece, new ChessPosition(column, row).toPosition());
	  piecesOnTheBoard.add(piece);
   }

   private void initialSetup() {
	  placeNewPiece('h', 1, new Rook(board, Color.WHITE));
	  placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
	  placeNewPiece('b', 1, new Knight(board, Color.WHITE));
	  placeNewPiece('e', 1, new King(board, Color.WHITE, this));
	  placeNewPiece('d', 1, new Queen(board, Color.WHITE));
	  placeNewPiece('g', 1, new Knight(board, Color.WHITE));
	  placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
	  placeNewPiece('a', 1, new Rook(board, Color.WHITE));
	  placeNewPiece('a', 2, new Pawn(board, Color.WHITE, this));
	  placeNewPiece('b', 2, new Pawn(board, Color.WHITE, this));
	  placeNewPiece('c', 2, new Pawn(board, Color.WHITE, this));
	  placeNewPiece('d', 2, new Pawn(board, Color.WHITE, this));
	  placeNewPiece('e', 2, new Pawn(board, Color.WHITE, this));
	  placeNewPiece('f', 2, new Pawn(board, Color.WHITE, this));
	  placeNewPiece('g', 2, new Pawn(board, Color.WHITE, this));
	  placeNewPiece('h', 2, new Pawn(board, Color.WHITE, this));

	  placeNewPiece('a', 8, new Rook(board, Color.BLACK));
	  placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
	  placeNewPiece('b', 8, new Knight(board, Color.BLACK));
	  placeNewPiece('e', 8, new King(board, Color.BLACK, this));
	  placeNewPiece('d', 8, new Queen(board, Color.BLACK));
	  placeNewPiece('g', 8, new Knight(board, Color.BLACK));
	  placeNewPiece('f', 8, new Bishop(board, Color.BLACK));
	  placeNewPiece('h', 8, new Rook(board, Color.BLACK));
	  placeNewPiece('a', 7, new Pawn(board, Color.BLACK, this));
	  placeNewPiece('b', 7, new Pawn(board, Color.BLACK, this));
	  placeNewPiece('c', 7, new Pawn(board, Color.BLACK, this));
	  placeNewPiece('d', 7, new Pawn(board, Color.BLACK, this));
	  placeNewPiece('e', 7, new Pawn(board, Color.BLACK, this));
	  placeNewPiece('f', 7, new Pawn(board, Color.BLACK, this));
	  placeNewPiece('g', 7, new Pawn(board, Color.BLACK, this));
	  placeNewPiece('h', 7, new Pawn(board, Color.BLACK, this));
   }
}
