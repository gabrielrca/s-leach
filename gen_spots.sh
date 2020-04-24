#!/bin/bash

echo '<?xml version="1.0" encoding="UTF-8"?>'
echo '<!-- first version using hand generated XML -->'
echo '<virtual-config keep-addresses="true" run-midlets="true">'
echo '	<gridview x="3767.189355784738" y="1881.4273623019471" zoom="0.13383269594045835"/>'
for i in `seq 1000 1049` ; 
do
 	echo "	<virtual-spot address=\"7f00.0101.0000.$i\">"
    echo '		<build file="../NetBeansProjects/projetoLeach/build.xml"/>'
    echo '		<midlet name="org.sunspotworld.DataDensityCorrelationDegreeApp"/>'
    echo '		<position x="-3386.116557081724" y="-1642.3228611549578"/>'
  	echo '	</virtual-spot>'
done
#Cria uma BaseStation
echo '	<virtual-spot address="7f00.0101.0000.2000">'
echo '		<build file="../NetBeansProjects/projetoLeach/build.xml"/>'
echo '		<midlet name="org.sunspotworld.BaseStation"/>'
echo '		<position x="-3386.116557081724" y="-1642.3228611549578"/>'
echo '	</virtual-spot>'

echo '</virtual-config>'
