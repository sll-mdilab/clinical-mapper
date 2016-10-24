package net.sllmdilab.clinicalmapper.util;

public class ClinisoftBodyTableGenerator {
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		StringBuffer table = new StringBuffer();
		table.append("id,vertical,horizontal,label\n");
		for(int i=66; i<=84; i++){
			Character c = (char)i;
			String col = c.toString();
			
			for(int j=1; j<=18; j++){	
				Integer irow = new Integer(j);
				String row = irow.toString();
				
				String line = col + row + "," + col + ",\"" + row + "\",";
				table.append(line);
				table.append("\n");
			}
		}
		System.out.println(table);
	}

}
