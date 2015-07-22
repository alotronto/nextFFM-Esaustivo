
public class OneSolution {

	private int[][] matrix;
	private int n;
	private int m;
	private float costo;
	
	public OneSolution(int n, int m) {
		// TODO Auto-generated constructor stub
		this.n = n;
		this.m = m;
		matrix = new int[n][m];
	}
	/**
	 * Metodo per il set di un particolare elemento in una soluzione
	 * @param n riga->squadra
	 * @param m colonna->intervento
	 */
	public void setElementNoOrder(int n,int m){
		matrix[n][m]=1;
	}
	/**
	 * Metodo per il set di un particolare elemento in una soluzione
	 * @param n riga->squadra
	 * @param m colonna->intervento
	 * @param order ordine di esecuzione dell'intervento
	 */
	public void setElementInOrder(int n,int m,int order){
		matrix[n][m]=order;
	}
	
	/**
	 * Metodo per il set di una intera colonna cioe' a quale squadra viene assegnato un
	 * determinato intervento
	 * @param index colonna->intervento
	 * @param col maschera di assegnazione del tipo "001..0"
	 */
	public void setColum(int index,String col){
		for(int i=0;i<n;i++){
			matrix[i][index] = Character.getNumericValue(col.charAt(i));
		}
	}
	
	/**
	 *Metodo per il set del costo TOTALE della soluzione 
	 * @param costo costo della soluzione TOTALE
	 */
	public void setCost(float costo){
		this.costo = costo;
	}
	/**
	 * Metdo che ritorna tutti gli interventi di una squadra
	 * in ordine di esecuzione
	 */
	public void getInterventOfOne(int squadra){
		
		String interventi = new String();
		for(int i=0;i<m;i++){
			interventi += String.valueOf(matrix[squadra][i])+"-";
		}
		
	}
	
	/**
	 * Metodo che restituisce la mastrice degli interventi
	 * @return
	 */
	public int[][] getMatrix(){
		return matrix;
	}
	
	/**
	 * 
	 */
	public String toString(){
		String comodo = new String();
		comodo+="-->";
		for(int i=0 ; i<n; i++){
			for (int j=0; j<m; j++){
				comodo +=matrix[i][j]; 
			}
			comodo+="|";
		}
		comodo+="<--";
		return comodo;
		
	}
	
}
