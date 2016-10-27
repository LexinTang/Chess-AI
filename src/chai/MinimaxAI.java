package chai;

import java.util.HashMap;

import chesspresso.move.IllegalMoveException;
import chesspresso.position.Position;

/* The framework of the code is based on: Artificial Intelligence A Modern Approach (3rd Edition): page 169.
 * function MINIMAX-DECISION(state) returns an action
 *   return argmax_[a in ACTIONS(s)] MIN-VALUE(RESULT(state, a))
 * 
 * function MAX-VALUE(state) returns a utility value
 *   if TERMINAL-TEST(state) then return UTILITY(state)
 *   v = -infinity
 *   for each a in ACTIONS(state) do
 *     v = MAX(v, MIN-VALUE(RESULT(s, a)))
 *   return v
 * 
 * function MIN-VALUE(state) returns a utility value
 *   if TERMINAL-TEST(state) then return UTILITY(state)
 *     v = infinity
 *     for each a in ACTIONS(state) do
 *       v  = MIN(v, MAX-VALUE(RESULT(s, a)))
 *   return v
 */

public class MinimaxAI implements ChessAI {
    private int nodesExplored = 0;
    private int maxDepth = 0;
    private final static int WIN = Integer.MAX_VALUE;
    private final static int LOSS = Integer.MIN_VALUE;
    private final static int DRAW = 0;
    private final static int MAX_DEPTH = 4;
    private int CUTOFF_DEPTH = 0;
    
    private boolean transTable_flag = true;
    private HashMap<Long, TransTableNode> transTable = new HashMap<Long, TransTableNode>();
    
    // return best move
	public short getMove(Position position) {
		Node bestMoveNode = null;
		Position pos = new Position(position);
		// iterative deepening
		for (int i = 1; i < MAX_DEPTH; i++){ 
			CUTOFF_DEPTH = i;
			resetStats();
			try {
				bestMoveNode = makeDecision(pos);
				
				printStats();
				if (bestMoveNode.utility == WIN){
					break;
				}
			} catch (IllegalMoveException e) {
				e.printStackTrace();
			}
		}
		
		short move = bestMoveNode.move;
		System.out.println("utility of best move = " + bestMoveNode.utility);
		return move;	
	}
	
	class Node{
		public short move;
		public int utility;
		public Node(short move, int utility){
			this.move = move;
			this.utility = utility;
		}
	}
	
	// node in transposition table
	class TransTableNode{
		public int utility;
		public int depth;
		public TransTableNode(int utility, int depth){
			this.utility = utility;
			this.depth = depth;
		}
	}
	
	private Node makeDecision(Position position) throws IllegalMoveException{
		int player = position.getToPlay();
		int bestUtility = LOSS;
		short bestMove = 0;
		short [] moves = position.getAllMoves();
		for (int i = 0; i < moves.length; i++){
			position.doMove(moves[i]);
			int new_utility = minValue(position, player, 1);
			if (bestUtility < new_utility){
				bestMove = moves[i];
				bestUtility = new_utility;
			}
			position.undoMove();
		}
		
		return (new Node(bestMove, bestUtility));
	}
	
	// return a utility value
	private int maxValue(Position position, int player, int depth) throws IllegalMoveException{
		updateDepth(depth);
		
		long hashPos = position.getHashCode();
		if (transTable_flag && transTable.containsKey(hashPos) && transTable.get(hashPos).depth >= CUTOFF_DEPTH){
			return transTable.get(hashPos).utility;
		}
		
		incrementNodeCount();
		if (cutoffTest(position, depth)){
			int utility =  getUtility(position, player);

            if (transTable_flag && (!transTable.containsKey(hashPos) || transTable.get(hashPos).depth < depth)) {
            	transTable.put(hashPos, new TransTableNode(utility, depth));
            }
			return utility;
		}
		
		int result = LOSS;
		short [] moves = position.getAllMoves();
		for (int i = 0; i < moves.length; i++){
			position.doMove(moves[i]);
			result = Math.max(result, minValue(position, player, depth + 1));
			position.undoMove();
		}
		
		return result;
	}
	
	// return a utility value
	private int minValue(Position position, int player, int depth) throws IllegalMoveException{
		
		updateDepth(depth);
		
		long hashPos = position.getHashCode();
		if (transTable_flag && transTable.containsKey(hashPos) && transTable.get(hashPos).depth >= CUTOFF_DEPTH){
			return transTable.get(hashPos).utility;
		}
		
		incrementNodeCount();
		if (cutoffTest(position, depth)){
			int utility =  getUtility(position, player);

            if (transTable_flag && (!transTable.containsKey(hashPos) || transTable.get(hashPos).depth < depth)) {
            	transTable.put(hashPos, new TransTableNode(utility, depth));
            }
			return utility;
		}

		int result = WIN;
		short [] moves = position.getAllMoves();
		for (int i = 0; i < moves.length; i++){
			position.doMove(moves[i]);
			result = Math.min(result, maxValue(position, player, depth + 1));
			position.undoMove();
		}
		
		return result;
	}
	
	private int getUtility(Position position, int player){
		if (position.isMate()){
			if (position.getToPlay() == player){
				return LOSS;
			}
			else{
				return WIN;
			}
		}
		else if (position.isTerminal() || position.isStaleMate()){
			return DRAW;
		}
		else{
			return evaluateFunc(position, player);
		}
	}
	
	private int evaluateFunc(Position position, int player){
		if (position.getToPlay() == player){
			return position.getMaterial();
		}
		else{
			return -position.getMaterial();
		}
	}
	
	private boolean cutoffTest(Position position, int depth){
		return (position.isTerminal() || position.isStaleMate() || depth >= CUTOFF_DEPTH);
	}
	
	private void resetStats() {
		nodesExplored = 0;
		maxDepth = 0;
	}
	
	private void printStats() {
		System.out.println("Nodes explored during last search:  " + nodesExplored);
		System.out.println("Maximum depth explored during last search " + maxDepth);
	}
	
	private void updateDepth(int depth) {
		maxDepth = Math.max(depth, maxDepth);
	}
	
	private void incrementNodeCount() {
		nodesExplored++;
	}
	
}
