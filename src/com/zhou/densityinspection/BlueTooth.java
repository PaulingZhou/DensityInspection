package com.zhou.densityinspection;

public class BlueTooth {

	enum ServerOrClient{
		NONE,
		SERVICE,
		CLIENT
	};
	static boolean isOpen = false;
	static ServerOrClient serverOrClient = ServerOrClient.NONE;
	static String BlueToothAddress = "null";
}
