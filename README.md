
# The Instrument - a Java project

The "Instrument" is a project to develop a Java based application system based on a general purpose Signal Processing engine. In the first instance this platform provides a framework for the implementation of a Music Information Processing application. 

The purpose of my doing this "side" project was to:
* Strengthen and update my core Java skills as a calling card to employers, (hopefully you!)
* Be a fun and playful creative exercise to provide really useful tools to support my hobby as an experimental musician and film artist as this gives me joy.
([Spotify](https://open.spotify.com/artist/0q6YXdTHAKSurxHEoAxbDm?si=alkeorFsRVSHRuE1dqLCqw) and [YouTube](https://www.youtube.com/channel/UCC1zuBMO0TDeicMlU1xzCZA))

The work is based on ideas and techniques that I have taken from the _many_ wonderful insights in the rich field of "MIR", (Music Information Retrieval,) research and practice. I have also derived and built on many examples of Java signal processing code from here on Github and beyond. (Please see [Acknowledgements](#acknowledgements) section below)



## License

[MIT](https://choosealicense.com/licenses/mit/)



## Tech Stack

**Client:** Swing, React (Provided by [instrumentamper](https://github.com/jimomulloy/instrumentamper) project)

**Server:** Java17, Quarkus, AWS Lambda



## Instrument project - Maven modules as implemented:

![InstrumentModules drawio](https://github.com/jimomulloy/instrument/assets/2285093/3d9f97e6-cdbb-43bd-bb34-b0564e4d568d)

* instrument : Parent module
* instrument-core : Core signal processing framework and functionality (as per design above.)
* instrument-desktop : PC Java Desktop implementation of instrument API including Swing UI, MicroStream DB and local file store.
* instrument-cmd : Simple implementation of instrument API for command line operation.
* instrument-st : System tests for Core Instrument funtions.
* instrument-aws : AWS based Cloud Services parent module.
* instrument-s3handler : AWS Cloud Services implementation of instrument API including Lambda functions and S3 store.
* instrument-s3handler-cdk : AWS Cloud Services "CDK" infrastructure-as-code implementation of deployable AWS service components.
* instrument-s3handler-st : System tests for AWS Cloud Services implementation of instrument API.
* 


## Installation

Install my-project with mvn

```bash
  cd instrument
  mvn clean install
```


## Deployment

To deploy this project run

```bash
  cd instrument
  mvn clean install
```



## Documentation

[Please see project Github WIKI](https://github.com/jimomulloy/instrument/wiki)

![The Instrument Block Diagram](https://github.com/jimomulloy/instrument/blob/main/images/instrumentblocks.drawio.png)



## Demo/Usage/Examples

### Screen shots

![image](https://github.com/jimomulloy/instrument/assets/2285093/f3bcebb5-c716-4650-8e9e-b50bcd42f917)

![20230524_144242605](https://github.com/jimomulloy/instrument/assets/2285093/a47b47f2-2e88-419c-8966-ff578b85d427)



## Acknowledgements

 - "Meinard Muller" for his great and unique text book, "Fundamentals of Music processing" ([Amazon Books](https://www.amazon.co.uk/Fundamentals-Music-Processing-Algorithms-Applications/dp/3319357654/ref=sxts_rp_s_1_0?content-id=amzn1.sym.07198d44-a16f-4503-b71e-3f4c67470a0f%3Aamzn1.sym.07198d44-a16f-4503-b71e-3f4c67470a0f&crid=NBI9Y2UQQ7QS&cv_ct_cx=fundamentals+of+music+processing&keywords=fundamentals+of+music+processing&pd_rd_i=3319357654&pd_rd_r=24499a8c-5353-43e8-888c-b3085bd81b92&pd_rd_w=G3ide&pd_rd_wg=25G1C&pf_rd_p=07198d44-a16f-4503-b71e-3f4c67470a0f&pf_rd_r=1B9S6KSZG2MRASWSXJ6R&qid=1684838823&sbo=RZvfv%2F%2FHxDF%2BO5021pAnSA%3D%3D&sprefix=fundamentals+of+music+processing%2Caps%2C83&sr=1-1-1890b328-3a40-4864-baa0-a8eddba1bf6a)) 
 - The developers of the Java project, "Tarsos" which I have used as the core library for Java audio signal processing. ([Tarsos on Github](https://github.com/JorenSix/Tarsos))


