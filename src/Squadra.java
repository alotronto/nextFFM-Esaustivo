import java.util.ArrayList;

/**
 * Classe che astrare l'entita' squadra
 * @author Andrea Rocco Lotronto
 *
 */
public class Squadra {

	private String id;
	private ArrayList<String> competenzeSquadra;
	private String alimentazioneAutomezzo;
	private float consumoAutomezzo;
	private float costiProporzionali;
	private float costoOrarioSquadra;
	//private ArrayList<Intervento> listaInterventi;
	//private ArrayList<String> competenzeInterventi;
	
	public Squadra(String id) {
		this.id = id;
		competenzeSquadra = new ArrayList<String>();

	}
	
	/**
	 * Metdo per il set del costo orario totale della squadra
	 * dato dalla somma del costo orario di ciascun membro della squadra
	 * @param costoOrario
	 */
	public void setCostoOrarioSquadra(float costoOrario){
		this.costoOrarioSquadra = costoOrario;
	}
	
	/**
	 * Metdo get per il costo orario totale della quadra
	 * @return
	 */
	public float getCostoOrarioSquadra(){
		return this.costoOrarioSquadra;
	}
	
	/**
	 * Metodo get per l'alimentazione dell'automezzo affidato alla squadra
	 * @return String Benzina/Diesel
	 */
	public String getAlimentazioneAutomezzo(){
		return this.alimentazioneAutomezzo;
	}
	
	/**
	 * Metodo set per i costi proporzionali dell'automezzo affidato alla squadra
	 * @param usura costo proporzionale automezzo
	 */
	public void setCostoProporzionale(float usura){
		costiProporzionali = usura;
	}
	
	/**
	 * Metodo get per i costi proporzionali dell'automezzo affidato alla squadra
	 * @return
	 */
	public float getCostoProporzionale(){
		return costiProporzionali;
	}
	/**
	 * Metodo set per il costo orario totale della squadra
	 */
	public void setCostoOrarioSquadra(){
		costoOrarioSquadra = Utility.getCostoOrarioSquadra(id);
	}
	
	/**
	 * Metodo set per il tipo di alimentazione dell'automezzo utilizzato dalla squadra
	 * @param alimentazione String benzina/diesel
	 */
	public void setAlimentazioneAuto(String alimentazione){
		alimentazioneAutomezzo = alimentazione;
	}
	
	/**
	 * Metodo set per il consumo dell'automezzo della squadra
	 * @param consumo float consumo per 100 km
	 */
	public void setConsumo(float consumo){
		consumoAutomezzo = consumo;
	}
	
	/**
	 * Metodo get per il consumo dell'automezzo della squadra
	 * @return float consumo per 100 km
	 */
	public float getConsumo(){
		return this.consumoAutomezzo;
	}
	/**
	 * Metodo get per Id della squadra
	 * 
	 * @return id squadra
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Metodo per il setting delle competenze di un intervento
	 * 
	 * @param compentenza String competenza
	 */
	public void addCompentenza(String compentenza){
		competenzeSquadra.add(new String(compentenza.toString()));
		
	}
	
	/**
	 * Metodo per il get delle competenze della squadra
	 * @return ArrayList<String> delle competenze della squadra
	 */
	public ArrayList<String> getCompentenze(){
		return competenzeSquadra;
	}
	
	/**
	 * Metodo che testa se tutte le competente passate sono in possesso della squadra
	 * @param competenze ArrayList<String> contente le competenze da testare
	 * @return true se si possiedono tutte le compentenze false altrimenti
	 */
	public boolean testCompetenze(ArrayList<String> competenze){
		
		if(this.competenzeSquadra.containsAll(competenze))
			return true;
		else
			return false;
				
	}
}
