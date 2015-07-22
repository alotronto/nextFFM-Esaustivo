import java.util.ArrayList;


public class solutionInOrder {
	
	private ArrayList<String> listaInterventi;
	private float costoTotale;
	
	
	/**
	 * Costruttore
	 */
	public solutionInOrder() {
		// TODO Auto-generated constructor stub
		listaInterventi = new ArrayList<String>();
		costoTotale = 0;
	}
	
	/**
	 * Metodo per il set della lista di interventi
	 * viene memorizzata la lista in ordine degli id degli interventi
	 * in particolare il primo elemento dell'ArrayList corispondo alla lista della prima squadra
	 * e cosi' via
	 * @param listaInterventi
	 */
	public void setListaInterventi(ArrayList<String> lista){
		listaInterventi.clear();
		for(int i =0; i<lista.size();i++)
		listaInterventi.add(new String(lista.get(i)));
	}
	
	/**
	 * Metodo per il set del costo totale della soluzione
	 * @param costo
	 */
	public void setCostoTotale(float costo){
		this.costoTotale = costo;
	}
	
	/**
	 * Metodo per il get del costo TOTALE
	 * @return Costo TOTALE della soluzione
	 */
	public float getCosto(){
		return costoTotale;
	}
	
	/**
	 * Metdo per il get della lista di Interventi
	 * @return lista interventi ordinata della soluzione
	 */
	public ArrayList<String> getListaInterventi(){
		return listaInterventi;
	}
	
	public String toString(){
		String result = new String();
		result = "Lista interventi==>";
		for(int i=0;i<listaInterventi.size();i++){
			result+="squadra "+(i+1)+":"+listaInterventi.get(i)+"--";
		}
		result+="Costo TOTALE::"+costoTotale;
		return result;
	}
	
}
