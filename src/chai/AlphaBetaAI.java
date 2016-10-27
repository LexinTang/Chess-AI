package chai;

import java.util.HashMap;

import chesspresso.move.IllegalMoveException;
import chesspresso.position.Position;

/* The framework of the code is based on: Artificial Intelligence A Modern Approach (3rd Edition): Page 173.
 * function ALPHA-BETA-SEARCH(state) returns an action
 *   v = MAX-VALUE(state, -infinity, +infinity)
 *   return the action in ACTIONS(state) with value v
 *   
 * function MAX-VALUE(state, alpha, beta) returns a utility value
 *   if TERMINAL-TEST(state) then return UTILITY(state)
 *   v = -infinity
 *   for each a in ACTIONS(state) do
 *     v = MAX(v, MIN-VALUE(RESULT(s, a), alpha, beta))
 *     if v >= beta then return v
 *     alpha = MAX(alpha, v)
 *   return v
 *   
 * function MIN-VALUE(state, alpha, beta) returns a utility value
 *   if TERMINAL-TEST(state) then return UTILITY(state)
 *   v = infinity
 *   for each a in ACTIONS(state) do
 *     v = MIN(v, MAX-VALUE(RESULT(s,a), alpha, beta))
 *     if v <= alpha then return v
 *     beta = MIN(beta, v)
 *   return v
 */

public class AlphaBetaAI implements ChessAI{

    private int nodesExplored = 0;
    private int maxDepth = 0;
    private final static int WIN = Integer.MAX_VALUE;
    private final static int LOSS = Integer.MIN_VALUE;
    private final static int DRAW = 0;
    private final static int MAX_DEPTH = 6;
    private int CUTOFF_DEPTH = 0;
    
    private boolean transTable_flag = false;
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
		public int bound; 
		public TransTableNode(int utility, int depth, int bound){
			this.utility = utility;
			this.depth = depth;
			this.bound = bound;	// bound = -1: lower bound, 1 : upper bound, 0 : not a bound
		}
	}
	
	private Node makeDecision(Position position) throws IllegalMoveException{
		int player = position.getToPlay();
		int bestUtility = LOSS;
		short bestMove = 0;
		short [] moves = position.getAllMoves();
		for (int i = 0; i < moves.length; i++){
			position.doMove(moves[i]);
			int new_utility = minValue(position, player, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (bestUtility < new_utility){
				bestMove = moves[i];
				bestUtility = new_utility;
			}
			position.undoMove();
		}
		
		return (new Node(bestMove, bestUtility));
	}
	
	// return a utility value
	private int maxValue(Position position, int player, int depth, int alpha, int beta) throws IllegalMoveException{
		updateDepth(depth);
		long hashPos = position.getHashCode();
		if (transTable_flag && transTable.containsKey(hashPos) && transTable.get(hashPos).depth >= CUTOFF_DEPTH){
            //System.out.println("trying to return from table");
			int utility = transTable.get(hashPos).utility;;
            if (transTable.get(hashPos).bound == 0){
                return utility;
            }
            else if (transTable.get(hashPos).bound == 1 && utility <= alpha){
                return utility;
            }
            else if (transTable.get(hashPos).bound == -1 && utility >= beta){
                return utility;
            }
        }
		
		incrementNodeCount();
		if (cutoffTest(position, depth)){
			int utility = getUtility(position, player);
			if (transTable_flag && (!transTable.containsKey(hashPos) || transTable.get(hashPos).depth < depth)) {
				if (utility <= alpha){
					transTable.put(hashPos, new TransTableNode(utility, depth, 1)); //upper bound
	            } 
				else if (utility >= beta){
	            	transTable.put(hashPos, new TransTableNode(utility, depth, -1));//lower bound
	            }
				else {
	            	transTable.put(hashPos, new TransTableNode(utility, depth, 0));	// not a bound
	            }
            }
			return utility;
		}
		
		int result = LOSS;
		short [] moves = position.getAllMoves();
		for (int i = 0; i < moves.length; i++){
			position.doMove(moves[i]);
			result = Math.max(result, minValue(position, player, depth + 1, alpha, beta));
			position.undoMove();
			
			//alpha-beta pruning
            if(result >= beta){
                return result;
            }
            alpha = Math.max(alpha, result);
		}
		
		return result;
	}
	
	// return a utility value
	private int minValue(Position position, int player, int depth, int alpha, int beta) throws IllegalMoveException{
		updateDepth(depth);
		long hashPos = position.getHashCode();
		if (transTable_flag && transTable.containsKey(hashPos) && transTable.get(hashPos).depth >= CUTOFF_DEPTH){
            int utility = transTable.get(hashPos).utility;;
            if (transTable.get(hashPos).bound == 0){
                return utility;
            }
            else if (transTable.get(hashPos).bound == 1 && utility <= alpha){
                return utility;
            }
            else if (transTable.get(hashPos).bound == -1 && utility >= beta){
                return utility;
            }
        }
		
		incrementNodeCount();
		if (cutoffTest(position, depth)){
			int utility = getUtility(position, player);
			if (transTable_flag && (!transTable.containsKey(hashPos) || transTable.get(hashPos).depth < depth)) {
				if (utility <= alpha){
					transTable.put(hashPos, new TransTableNode(utility, depth, 1)); // upper bound
	            } 
				else if (utility >= beta){
	            	transTable.put(hashPos, new TransTableNode(utility, depth, -1));// lower bound
	            }
				else {
	            	transTable.put(hashPos, new TransTableNode(utility, depth, 0));	// not a bound
	            }
            }
			return utility;
		}
		
		int result = WIN;
		short [] moves = position.getAllMoves();
		for (int i = 0; i < moves.length; i++){
			position.doMove(moves[i]);
			result = Math.min(result, maxValue(position, player, depth + 1, alpha, beta));
			position.undoMove();
			
			//alpha-beta pruning
            if(result <= alpha){
                return result;
            }
            beta = Math.min(beta, result);
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
		return (position.isTerminal() || depth >= CUTOFF_DEPTH);
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
