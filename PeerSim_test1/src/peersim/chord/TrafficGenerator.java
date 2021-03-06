/**
 * 
 */
package peersim.chord;

import peersim.core.*;
import peersim.config.Configuration;
import peersim.edsim.EDSimulator;

/**
 * @author Andrea
 * 
 */
public class TrafficGenerator implements Control {

	private static final String PAR_PROT = "protocol";

	private final int pid;

	/**
	 * 
	 */
	public TrafficGenerator(String prefix) {
		pid = Configuration.getPid(prefix + "." + PAR_PROT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see peersim.core.Control#execute()
	 */
	public boolean execute() {
		//ԭΪ
		//int size = Network.size();
		//�޸�
		int size = Network.dy_size;
		//end
		Node sender, target;
		int i = 0;
		do {
			i++;
			sender = Network.get(CommonState.r.nextInt(size));
			target = Network.get(CommonState.r.nextInt(size));
		} while (sender == null || sender.isUp() == false || target == null
				|| target.isUp() == false);
		LookUpMessage message = new LookUpMessage(sender,
				((ChordProtocol) target.getProtocol(pid)).chordId);
		//test
//		System.out.println("from:"+((ChordProtocol) sender.getProtocol(pid)).chordId+" to:"+((ChordProtocol) target.getProtocol(pid)).chordId);
		
		EDSimulator.add(10, message, sender, pid);
		return false;
	}

}
