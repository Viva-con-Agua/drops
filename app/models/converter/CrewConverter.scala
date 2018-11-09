package models.converter

import models.{Crew, CrewStub, City}
import models.database.{CityDB, CrewDB}

object CrewConverter {
  /**
    * Convert the db result list to an crew object.
    * Important: This impl works only for one crew and not for a lit of crews!
    * @param result
    * @return
    */
  def buildCrewObjectFromResult(result : Seq[(CrewDB, CityDB)]) : Option[Crew] = {
    if(result.headOption.isDefined) {
      val crew = result.headOption.get._1
      val cityList = result.seq.foldLeft(Set[City]()) { (cityList, dbEntry) => {
        if (crew.id == dbEntry._2.crewId)
          cityList ++ List(dbEntry._2.toCity)
        else cityList
      }}

      Option(Crew(crew.publicId, crew.name, cityList))
    }else{
      None
    }
  }

  def buildCrewListFromResult(result : Seq[(CrewDB, CityDB)]) : List[Crew] = {
    val crewList = result.seq.foldLeft[List[Crew]](Nil)((crewList, dbEntry) =>{
      val city = dbEntry._2
      val crew = dbEntry._1

      if(crewList.length != 0 && crewList.last.id == dbEntry._1.publicId){
        //tail = use all elements except the head element
        //reverse.tail.reverse = erease last element from list
        crewList.reverse.tail.reverse ++ List(crewList.last.copy(cities = crewList.last.cities ++ List(city.toCity) ))
      }else{
        crewList ++ List(crew.toCrew(Set(city.toCity)))
      }
    })
    crewList
  }

  def buildCrewStubListFromCrewList(crewList : List[Crew]) : List[CrewStub] = {
    val crewStubList : List[CrewStub] = List[CrewStub]()
    crewList.foreach(crew => {
      crewStubList ++ List(crew.toCrewStub())
    })

    crewStubList
  }
}
