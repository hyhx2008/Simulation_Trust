package peersim.chord;

import java.math.*;
import peersim.core.*;

public class LookUpMessage implements ChordMessage {

	private Node sender;

	private BigInteger targetId;

	private int hopCounter = 0;
	
	public Node[] hops; 
	
	public int hops_index = 0;
	
	public LookUpMessage(Node sender, BigInteger targetId) {
		this.sender = sender;
		this.targetId = targetId;
		this.hops = new Node[3];
	}

	public void increaseHopCounter() {
		hopCounter++;
	}

	public void add_hop(Node n)
	{
		if (hops_index < Flag.look_ahead_level + 1)
		{
			hops[hops_index] = n;
			hops_index ++;
		}else
		{
			for (int i=1;i<hops_index;++i)
			{
				hops[i-1] = hops[i];
			}
			hops[hops_index -1] = n;
		}
	}
	/**
	 * @return the senderId
	 */
	public Node getSender() {
		return sender;
	}

	/**
	 * @return the target
	 */
	public BigInteger getTarget() {
		return targetId;
	}

	/**
	 * @return the hopCounter
	 */
	public int getHopCounter() {
		return hopCounter;
	}

}
