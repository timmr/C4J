package de.andrena.c4j.acceptancetest.lesson202;

import de.andrena.c4j.ContractReference;
import de.andrena.c4j.Pure;
import de.andrena.c4j.acceptancetest.lesson201.ASpec;

@ContractReference(BSpecContract.class)
public interface BSpec extends ASpec {

	@Pure
	int queryB();

	void commandB(int value);

	@Override
	@Pure
	int query(int x, int y);

	@Override
	void command(int value);

}