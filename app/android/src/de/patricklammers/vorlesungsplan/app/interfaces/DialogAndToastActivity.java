package de.patricklammers.vorlesungsplan.app.interfaces;

public interface DialogAndToastActivity {
	/**
	 * Zeigt einen ProgressDialog mit Standard-Titel und -Text an, der
	 * nicht abgebrochen werden kann.
	 */
	public void showWaitDialog();
	
	/**
	 * Diese Methode erzeugt einen ProgressDialog, bei dem der Titel
	 * und der Text als parameter uebergeben werden. Es kann imemr nur
	 * ein ProgressDialog gleichzeitig angezeigt werden. Der ProgressDialog
	 * kann nicht abgebrochen werden.
	 *  
	 * @param title
	 * @param text
	 */
	public void showWaitDialog(final String title, final String text);

	/**
	 * Diese Methode erzeugt einen ProgressDialog, bei dem der Titel
	 * und der Text als parameter uebergeben werden. Es kann imemr nur
	 * ein ProgressDialog gleichzeitig angezeigt werden. Dieser
	 * ProgressDialog kann vom Benutzer abgebrochen werden.
	 *  
	 * @param title
	 * @param text
	 * @param thread Ein Thread, der abgebrochen wird, wenn der Benutzer
	 * 				 den ProgressDialog schließt
	 */
	public void showWaitDialog(final String title, final String text, final CancelableThread thread);
	
	/**
	 * Diese Methode ändert den Text des ProgressDialogs
	 *  
	 * @param newText
	 */
	public void changeDialogText(final String newText);
	
	/**
	 * Diese Methode versteckt den ProgressDialog wieder
	 */
	public void hideWaitDialog();
	
	/**
	 * Diese Methode erzeugt einen Toast mit dem gewünschten Text und der gewünschten Anzeigedauer  
	 * 
	 * @param text
	 * @param duration
	 */
	public void showToast(final String text, final int duration);
}
